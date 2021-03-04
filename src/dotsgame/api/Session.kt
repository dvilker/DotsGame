package dotsgame.api

import dotsgame.entities.User
import dotsgame.entities.isSysAdmin
import dotsgame.entities.newUser
import dotsgame.robots.makeMimeMessageOrNull
import dotsgame.robots.sendImmediately
import dotsgame.server.*
import zApi.api.ApiMethod
import zApi.api.dialogs.Captcha
import zApi.api.dialogs.ask
import zApi.api.servlet.ApiDirect
import zDb.find
import zDb.finder.eq
import zDb.finder.setTo
import zDb.finder.updateValues
import zDb.getDb
import zDb.transactionDb
import zUtils.*
import zUtils.log.logHint
import zUtils.log.logInfo
import zUtils.log.logWarning
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLEncoder
import java.security.Key
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.mail.Message
import javax.mail.internet.InternetAddress
import kotlin.random.asKotlinRandom

class SessionInfo(
    val user: User?,
    val menu: Any?,
    val label: String,
    val icon: String,
    val isDevil: Boolean
)

@ApiMethod(needAuth = false)
fun login(login: String, password: String): SessionInfo {
    try {
        val user = if (login.isNotEmpty() && password.isNotEmpty()) {
            // Вход по логину и паролю
            Context.getUserByLoginPassword(login, password, withToken = true)
        } else if (login.isEmpty()) {
            badParam("login", "Укажите имя пользователя")
        } else {
            badParam("password", "Укажите пароль")
        }
        if (user == null) {
            badParam("password", "Имя пользователя или пароль указаны неверно")
        }
        if (user.token == null) {
            user.token = genToken()
            getDb().save(user)
        }
        val context = threadContext
        Context.setUserCookie(
            context.http ?: badAlgorithm(),
            user.id,
            user.token ?: badAlgorithm()
        )
        logHint("Вход {0} на {1}", login, context.http.remoteAddr)
        user.unsetValue(User::token)
        return getSessionInfo(user)
    } catch (t: Throwable) {
        logHint("Неудачная попытка входа {0}: {1}", login, t.message)
        throw t
    }
}

private fun registerCheck(name: String, email: String, password1: String, password2: String): Triple<String, String, String> {
    val name0 = name.nullIfBlank()?.normalizeName2() ?: badParam("name", "Как вас зовут?")
    if (!Regex("""([A-Z][a-z]+ [A-Z][a-z]+|[А-ЯЁ][а-яё]+ [А-ЯЁ][а-яё]+)""").matches(name0)) {
        badParam("name", "Здесь что-то не то")
    }
    if (name0.length > 50) {
        badParam("name", "Слишком длинно")
    }
    val email0 = (
            email.nullIfBlank()?: badParam("email", "Без адреса почты никак")
            ).normalizeEMail() ?: badParam("email", "Мы такой адрес не принимаем")
    if (email0.contains('+')) {
        badParam("email", "Без плюсов, пожалуйста")
    }
    if (email0.length > 50) {
        badParam("email", "Слишком длинно")
    }
    if (password1.isBlank()) {
        badParam("password1", "Ну хотя бы 123")
    }
    if (password1.any { it < ' ' }) {
        badParam("password1", "Что-то непонятное вы ввели")
    }
    if (password1.length > 50) {
        badParam("password1", "Слишком длинно")
    }
    if (password2.isBlank()) {
        badParam("password2", "Ещё раз пароль, пожалуйста")
    }
    if (password1 != password2) {
        badParam("password2", "Нужно, чтобы совпало")
    }
    return Triple(name, email, password1)
}

private fun genToken(): String {
    return generateCode(20, random = SecureRandom().asKotlinRandom())
}

@ApiMethod(needAuth = false)
fun register(name: String, email: String, password1: String, password2: String) {
    Captcha().ask()
    val (name0, email0, password0) = registerCheck(name, email, password1, password2)
    if (getDb().find(User::email eq email0).exists()) {
        badParam("email", "Пользователь с таким адресом уже есть")
    }
    val mailCode = packMailCode(
        "R1",
        System.currentTimeMillis(),
        "$name0\n$email0\n$password0"
    )
    val link = """${System.getProperty("dots.url")}a/confirm?m=${URLEncoder.encode(mailCode, "UTF-8")}"""
    logInfo("Ссылка регистрации для {0}: {1}", email0, link)

    try {
        val message = makeMimeMessageOrNull() ?: badConfig("Почта не настроена")
        message.subject = "Подтверждение регистрации"
        message.addRecipient(
            Message.RecipientType.TO, InternetAddress(
                email0,
                name0,
                "utf-8"
            )
        )
        message.setText(
            """
            Добрый день.
            
            Рады приветствовать в наших рядах!
            
            Для завершения регистрации осталась небольшая формальность, пройдите по ссылке:
            
            $link
            
            Ссылка одноразовая и будет действовать не более 10 минут.
            """.trimIndent(),
            "utf-8",
            "plain"
        )
        message.sendImmediately()
    } catch (t: Throwable) {
        logWarning("Не удалось отправить письмо о регистрации для {0}: {1}", email0, t)
        badUnknownParam("Не удалось отправить письмо с подтверждением")
    }
}

@ApiMethod(needAuth = false, needSecret = false)
fun confirm(m: String): ApiDirect {
    try {
        val parsedMailCode = parseMailCode(m)
        if (System.currentTimeMillis() > parsedMailCode.time + 600_000) {
            badUnknownParam("10 min")
        }
        val context = threadContext
        when (parsedMailCode.type) {
            "R1" -> {
                val split = parsedMailCode.payload.split("\n")
                if (split.size != 3) {
                    badUnknownParam("split.size != 3")
                }
                val (name, email, password) = registerCheck(split[0], split[1], split[2], split[2])
                val user = getDb().transactionDb { db ->
                    if (db.find(User::email eq email).exists()) {
                        badUnknownParam("Уже зарегистрирован")
                    }
                    val maxIndex = db.query(
                        """
                            SELECT 
                                max(name_index)::integer
                            FROM "user" 
                            WHERE name = :name 
                        """,
                        mapOf(
                            "name" to name
                        )
                    ).use {
                        if (it.next()) {
                            it.getInt(0)
                        } else {
                            null
                        }
                    }
                    val nameIndex = maxIndex?.let { it + 1 } ?: 1
                    db.find(User::name eq email).exists()
                    val newUser = newUser(
                        active = true,
                        email = email,
                        token = genToken(),
                        title = if (nameIndex > 1) "$name $nameIndex" else name,
                        name = name,
                        nameIndex = nameIndex,
                        lastActionTime = parsedMailCode.time
                    )
                    db.acquireId(newUser)
                    newUser.password = Context.getUserPasswordHash(newUser.id, password)
                    db.save(newUser)
                    newUser
                }
                logInfo("Регистрация {0} -> {2}", split, user.id)
                Context.setUserCookie(
                    context.http ?: badAlgorithm(),
                    user.id,
                    user.token ?: badAlgorithm()
                )
                logHint("Вход {0} на {1}", user.email, context.http.remoteAddr)
                return ApiDirect.redirect("/E.htm#R1")
            }
            "P1" -> {
                val split = parsedMailCode.payload.split("\n")
                if (split.size != 2) {
                    badUnknownParam("split.size != 2")
                }
                val (email, password) = split
                val user = getDb().transactionDb { db ->
                    val user = db.find(
                        User::email,
                        User::title,
                        User::password,
                        User::token,
                        User::lastActionTime,
                        User::active eq true,
                        User::email eq email,
                    ).getOrNull() ?: badUnknownParam("!User {0}", email)
                    if (parsedMailCode.time <= user.lastActionTime) {
                        badUnknownParam("parsedMailCode.time <= user.lastActionTime")
                    }
                    if (password.isBlank() || password.any { it < ' ' } || password.length > 50) {
                        badUnknownParam("Invalid password")
                    }
                    user.password = Context.getUserPasswordHash(user.id, password)
                    user.lastActionTime = parsedMailCode.time
                    user.token = genToken()
                    db.save(user)
                    user
                }
                logInfo("Изменение пароля для: {0}", user.id)
                Context.setUserCookie(
                    context.http ?: badAlgorithm(),
                    user.id,
                    user.token ?: badAlgorithm()
                )
                logHint("Вход {0} на {1}", user.email, context.http.remoteAddr)
                return ApiDirect.redirect("/E.htm#P1")
            }
            else -> badUnknownParam("Type {0}?", parsedMailCode.type)
        }
    } catch (t: Throwable) {
        logWarning("Неверна ссылка: {0}", t)
        return ApiDirect.redirect("/E.htm#E1")
    }
}

@ApiMethod(needAuth = false)
fun resetPassword(email: String, password1: String, password2: String) {
    Captcha().ask()
    val email0 = email.normalizeEMail()
    if (email0.isNullOrBlank()) {
        badParam("email", "Куда слать-то?")
    }
    if (password1.isBlank()) {
        badParam("password1", "Ну хотя бы 123")
    }
    if (password1.any { it < ' ' }) {
        badParam("password1", "Что-то непонятное вы ввели")
    }
    if (password1.length > 50) {
        badParam("password1", "Слишком длинно")
    }
    if (password2.isBlank()) {
        badParam("password2", "Ещё раз пароль, пожалуйста")
    }
    if (password1 != password2) {
        badParam("password2", "Нужно, чтобы совпало")
    }
    val user = getDb().find(
        User::title,
        User::active eq true,
        User::email eq email,
    ).getOrNull() ?: return

    val mailCode = packMailCode(
        "P1",
        System.currentTimeMillis(),
        "$email\n$password1"
    )
    val link = """${System.getProperty("dots.url")}a/confirm?m=${URLEncoder.encode(mailCode, "UTF-8")}"""
    logInfo("Ссылка смены пароля для {0}: {1}", email0, link)

    try {
        val message = makeMimeMessageOrNull() ?: badConfig("Почта не настроена")
        message.subject = "Изменение пароля"
        message.addRecipient(
            Message.RecipientType.TO, InternetAddress(
                email0,
                user.title,
                "utf-8"
            )
        )
        message.setText(
            """
            Добрый день.
            
            Кто-то, возможно вы, запросил изменение пароля.
            
            Для того, чтобы подтвердить намерение, пройдите по ссылке:
            
            $link
            
            Ссылка одноразовая и будет действовать не более 10 минут.
            
            Если вы не запрашивали изменение — ничего не делайте.
            """.trimIndent(),
            "utf-8",
            "plain"
        )
        message.sendImmediately()
    } catch (t: Throwable) {
        logWarning("Не удалось отправить письмо о смене пароля для {0}: {1}", email0, t)
        badUnknownParam("Не удалось отправить письмо о смене пароля")
    }
}

@ApiMethod(needAuth = true)
fun logout() {
    val context = threadContext
    context.userId?.let { id ->
        val user = getDb().find(
            User::phone,
            User::email,
            User::id eq id,
            updateValues(
                User::token setTo null
            )
        ).getOrNull()
        logHint("Выход {0} с {1}", user?.phone ?: user?.email ?: id, context.http?.remoteAddr)
    }
    Context.unsetUserCookie(context.http ?: badAlgorithm())
}

@ApiMethod(needAuth = false)
fun session(): SessionInfo {
    return getSessionInfo()
}

fun getSessionInfo(
    overrideUser: User? = null
): SessionInfo {
    val context = threadContext
    return SessionInfo(
        overrideUser ?: context.user,
        null,
        appCommon.label,
        appCommon.icon,
        context.isDevil
    )
}


@ApiMethod(needAuth = false, needSecret = false)
fun verifyNumber(number: String, code: Int) {
    val numa = number
    val numb = 1_000_000_0000 + code
    logInfo("Вызов: {0} с номера {1}", numa, numb)
    val callSidRaw = URL(
        "${System.getProperty("dots.sipmarket.apiUrl")}callback.aspx?sid=yes&numa=$numa&numb=$numb" +
                "&login=${System.getProperty("dots.sipmarket.login")}&apicode=${System.getProperty("dots.sipmarket.apicode")}"
    ).readText().trim()
    val sid = (regex("^Success 200 ([a-z0-9]+)$").matchEntire(callSidRaw) ?: throw BadExternal(
        "Ошибка вызова: {0}",
        callSidRaw
    )).groupValues[1]
    logInfo("SID вызова: {0}", sid)
    try {
        var x = 100
        var y = Int.MAX_VALUE
        var errorCount = 3
        while (x-->0 && y-->0 && errorCount > 0) {
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
                    y = 5 // 0,5 sec
                }
            } else if (statusRaw.startsWith("Success numa: Connect /")) {
                logWarning("Взяли трубку, вешаем")
                break
            } else if (statusRaw != "Success numa: Trying / numb: Waiting") {
                logWarning("Непонятный статус: {0}", statusRaw)
                break
            }
        }
    } finally {
        var success = false
        var x = 10
        while (x-->0) {
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
                        success = true
                        break
                    }
                }
            } catch (t: Throwable) {
                logWarning("Сбой завершения звонка: {0}", t)
            }
        }
        if (!success) {
            logWarning("Сбой завершения звонка: не достигли необходимого состояния")
        }
    }
}





private fun getAesCipher(encrypt: Boolean):Cipher {
    val key = object : Key {
        override fun getAlgorithm(): String = "AES"
        override fun getFormat(): String = "RAW"
        override fun getEncoded(): ByteArray {
            val key = MessageDigest.getInstance("SHA-256")
            for(i in 1..7) {
                key.update(appSecretBytes)
            }
            return key.digest()
        }

    }
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, key, IvParameterSpec(ByteArray(16)))
    return cipher
}


fun packMailCode(type: String, time: Long, payload: String): String {
    val str = type + '\n' + time.toString(36) + '\n' + payload
    val strBytes = str.toByteArray(Charsets.UTF_8)

    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(strBytes)
    for(i in 1..11) {
        messageDigest.update(appSecretBytes)
    }
    val digest = messageDigest.digest()

    val out = ByteArrayOutputStream(256)
    out.write(strBytes)
    out.write(digest)
    val regCode = out.toByteArray()

    return getAesCipher(true).doFinal(regCode).toBase64()
}

data class ParsedMailCode(
    val type: String,
    val time: Long,
    val payload: String
)

fun parseMailCode(code: String): ParsedMailCode {
    val decrypted: ByteArray = getAesCipher(false).doFinal(code.fromBase64())
    if (decrypted.size < 32) {
        badUnknownParam("< 32")
    }
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(decrypted, 0, decrypted.size - 32)
    for (i in 1..11) {
        messageDigest.update(appSecretBytes)
    }
    val digest = messageDigest.digest()

    for (i in 1..32) {
        if (decrypted[decrypted.size - i] != digest[digest.size - i]) {
            badUnknownParam("Sign")
        }
    }
    val lines = String(decrypted, 0, decrypted.size - 32, Charsets.UTF_8).split("\n", limit = 3)
    if (lines.size != 3) {
        badUnknownParam("lines.size != 3")
    }
    return ParsedMailCode(
        lines[0],
        lines[1].toLongOrNull(36) ?: badUnknownParam("!long36"),
        lines[2]
    )
}
