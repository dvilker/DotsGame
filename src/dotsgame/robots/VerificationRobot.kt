package dotsgame.robots

import dotsgame.entities.Verification
import zDb.DB_NOW
import zDb.connectDb
import zDb.find
import zDb.finder.eq
import zUtils.BadExternal
import zUtils.VarWithLock
import zUtils.log.logInfo
import zUtils.log.logWarning
import zUtils.log.setLogMarker
import zUtils.regex
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

const val VERIFICATION_NOTIFY = "VERIFICATION"

class VerificationRobot : AbstractRobot() {
    private val thread: Thread = Thread(this::threadProc, "CallVerifyRobot")
    private val notified = VarWithLock(true)
    private val notificationListener = DbNotificationRobot.Listener("verification") { _, _ ->
        if (VERIFICATION_NOTIFY == VERIFICATION_NOTIFY) {
            notified.withLock {
                notified.value = true
                notified.cond.signal()
            }
        }
    }

    override fun start() {
        notificationRobot.addListener(notificationListener)
        thread.start()
    }

    override fun stop(join: Boolean) {
        notificationRobot.removeListener(notificationListener)
        thread.interrupt()
        if (join) {
            thread.join()
        }
    }

    private fun threadProc() {
        setLogMarker("CallVerifyRobot")
        val thread = Thread.currentThread()
        var lastCleanupTime = System.currentTimeMillis()
        while (!thread.isInterrupted) {
            try {
                connectDb().use { db ->
                    while (!thread.isInterrupted) {
                        notified.withLock {
                            notified.cond.await(30, TimeUnit.SECONDS)
                        }
                        val verifications = db.find(
                            Verification::sentTime eq null,
                            Verification::target,
                            Verification::code
                        ).getList()
                        for (verification in verifications) {
                            val error = try {
                                val target = verification.target
                                if (target.contains('@')) {
                                    "Проверка e-mail-ов не поддерживается"
                                } else {
                                    if (sendPhoneCode(target, verification.code.toInt())) {
                                        null
                                    } else {
                                        "Возможно звонок не прошел"
                                    }
                                }
                            } catch (t: Throwable) {
                                t.toString()
                            }
                            if (error != null) {
                                logWarning("Код {0} отправлен на {1} с ошибкой: {2}", verification.code, verification.target, error)
                            } else {
                                logInfo("Код {0} отправлен на {1}", verification.code, verification.target)
                            }
                            verification.sentTime = DB_NOW
                            verification.sentError = error
                            db.save(verification)
                        }
                        val now = System.currentTimeMillis()
                        if (now - lastCleanupTime > 300_000) {
                            db.execute("""DELETE FROM "verification" WHERE "sent_time" < now() - '24 hour'::interval """)
                            lastCleanupTime = now
                        }
                    }
                }
            } catch (t: InterruptedException) {
                break
            } catch (t: Throwable) {
                logWarning("Ошибка: {0}", t)
                try {
                    Thread.sleep(1000)
                } catch (t: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun sendPhoneCode(number: String, code: Int): Boolean {
        val numa = number
        val numb = 7_900_000_0000 + code
        logInfo("Вызов: {0} с номера {1}", numa, numb)
        val callSidRaw = URL(
            "${System.getProperty("dots.sipmarket.apiUrl")}callback.aspx?sid=yes&numa=$numa&numb=$numb" +
                    "&login=${System.getProperty("dots.sipmarket.login")}&apicode=${System.getProperty("dots.sipmarket.apicode")}"
        ).readText().trim()
        val sid = (
                regex("^Success 200 ([a-z0-9]+)$").matchEntire(callSidRaw)
                    ?: throw BadExternal("Ошибка вызова: {0}", callSidRaw)
                ).groupValues[1]
        logInfo("SID вызова: {0}", sid)
        try {
            var success = false
            var x = 100
            var y = Int.MAX_VALUE
            var errorCount = 3
            while (x-- > 0 && y-- > 0 && errorCount > 0) {
                Thread.sleep(100)
                val statusRaw = try {
                    URL(
                        "${System.getProperty("dots.sipmarket.apiUrl")}callback_status.aspx?&sid=$sid"
                    ).readText().trim()
                } catch (t: Throwable) {
                    logWarning("Сбой запроса статуса: {0}", t)
                    errorCount--
                    continue
                }
                logInfo("Статус вызова: {0}", statusRaw)
                if (statusRaw == "Success numa: Session Progress / numb: Waiting" || statusRaw == "Success numa: Ringing / numb: Waiting") {
                    if (y > 1000) {
                        success = true
                        y = 30 // 3 sec
                    }
                } else if (statusRaw.startsWith("Success numa: Connect /")) {
                    logWarning("Взяли трубку, вешаем")
                    break
                } else if (statusRaw != "Success numa: Trying / numb: Waiting") {
                    logWarning("Непонятный статус: {0}", statusRaw)
                    break
                }
            }
            return success
        } finally {
            var closeSuccess = false
            var x = 10
            while (x-- > 0) {
                try {
                    val closeRaw = URL(
                        "${System.getProperty("dots.sipmarket.apiUrl")}callback_cancelation.aspx?&sid=$sid"
                    ).readText().trim()
                    logInfo("Завершение звонка: {0}", closeRaw)
                    if (closeRaw == "Success") {
                        val statusRaw = try {
                            URL(
                                "${System.getProperty("dots.sipmarket.apiUrl")}callback_status.aspx?&sid=$sid"
                            ).readText().trim()
                        } catch (t: Throwable) {
                            logWarning("Сбой запроса статуса: {0}", t)
                            continue
                        }
                        logInfo("Статус вызова: {0}", statusRaw)
                        if (statusRaw == "Success numa: End connect / numb: End connect") {
                            closeSuccess = true
                            break
                        }
                    }
                } catch (t: Throwable) {
                    logWarning("Сбой завершения звонка: {0}", t)
                }
            }
            if (!closeSuccess) {
                logWarning("Сбой завершения звонка: не достигли необходимого состояния")
            }
        }
    }
}

fun verifyCall() {

}