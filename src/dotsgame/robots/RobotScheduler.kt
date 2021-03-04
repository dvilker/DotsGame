package dotsgame.robots

import zUtils.CronScheduler
import zUtils.ValWithLock
import zUtils.log.logDebug
import zUtils.log.logWarning
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.concurrent.withLock


class RobotScheduler : AutoCloseable {

    private val thread: Thread
    private val itemsVal = ValWithLock<MutableSet<Item>>(HashSet())

    /**
     * Добавляет вызов по расписанию.
     *
     * Обязательно! Вызов должен быть крайне быстрый. В основном тратить время только на запуск нового потока.
     */
    fun add(schedule: CronScheduler, callback: () -> Unit): Item {
        val item = Item(schedule, callback)
        itemsVal.withLock {
            itemsVal.value.add(item)
            itemsVal.cond.signal()
        }
        return item
    }

    /**
     * Добавляет вызов по расписанию.
     *
     * Обязательно! Вызов должен быть крайне быстрый. В основном тратить время только на запуск нового потока.
     */
    fun add(schedule: String, callback: () -> Unit): Item {
        return add(CronScheduler(schedule), callback)
    }

    private fun remove(item: Item) {
        itemsVal.withLock {
            itemsVal.value.remove(item)
            itemsVal.cond.signal()
        }
    }

    override fun close() {
        thread.interrupt()
        try {
            thread.join()
        } catch (e: InterruptedException) {
            //
        }
    }

    init {
        thread = thread(name = "Robot Scheduler") {
            val thread = Thread.currentThread()
            itemsVal.withLock {
                val items = itemsVal.value
                while (!thread.isInterrupted) {
                    var waitFor: LocalDateTime? = null
                    for (item in items) {
                        if (waitFor != null) {
                            logDebug("{0} < {1} = {2}", item.nextCall, waitFor, item.nextCall < waitFor)
                        }
                        if (waitFor == null || item.nextCall < waitFor) {
                            waitFor = item.nextCall
                        }
                    }
                    if (waitFor == null) {
                        try {
                            itemsVal.cond.await()
                            continue
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                    val between = ChronoUnit.MILLIS.between(LocalDateTime.now(), waitFor) + 1
                    if (between >= 0) {
                        try {
                            itemsVal.cond.await(between, TimeUnit.MILLISECONDS)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                    val now = LocalDateTime.now()
                    for (item in items) {
                        logDebug("{0} <= {1} = {2}", item.nextCall, now, item.nextCall <= now)
                        if (item.nextCall <= now) {
                            try {
                                item.callback()
                            } catch (e: Throwable) {
                                logWarning("Scheduler item {0} callback exception: {1}", item, e)
                            }
                            item.prepareNextCall(now)
                        }
                    }
                }
            }
        }
    }


    inner class Item internal constructor(internal val cronScheduler: CronScheduler, internal val callback: () -> Unit): AutoCloseable {
        private var lastCall: LocalDateTime = LocalDateTime.now()
        internal var nextCall: LocalDateTime = cronScheduler.getNextTime(lastCall)

        internal fun prepareNextCall(lastCall: LocalDateTime) {
            this.lastCall = lastCall
            nextCall = cronScheduler.getNextTime(lastCall)
        }

        override fun close() {
            remove(this)
        }
    }

}
