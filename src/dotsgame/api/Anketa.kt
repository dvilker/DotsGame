package dotsgame.api

import dotsgame.api.model.GRules
import dotsgame.api.model.GUser
import dotsgame.entities.User
import dotsgame.server.Context
import dotsgame.server.threadContext
import zApi.api.ApiMethod
import zDb.get
import zDb.getDb
import zDb.transactionDb
import zUtils.*
import zUtils.log.logWarning
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO

@ApiMethod
fun saveUser(
    name: Ref<String>? = null,
    pic: Ref<String?>? = null,
    rules: Ref<String>? = null
) {
    val user = getDb().transactionDb { db ->
        val user = db.get(
            threadContext.userId ?: badAlgorithm(),
            User::name,
            User::nameIndex,
            User::title,
            User::pic,
            User::level,
            User::score,
            User::rules,
        )
        if (name != null) {
            val name0 = name.value.nullIfBlank()?.normalizeName2() ?: badParam("name", "Как вас зовут?")
            if (!Regex("""([A-Z][a-z] [A-Z][a-z]+|[А-ЯЁ][а-яё]+ [А-ЯЁ][а-яё]+)""").matches(name0)) {
                badParam("name", "Здесь что-то не то")
            }
            if (name0.length > 50) {
                badParam("name", "Слишком длинно")
            }
            if (user.name != name0) {
                val maxIndex = db.query(
                    """
                        SELECT 
                            max(name_index)::integer
                        FROM "user" 
                        WHERE name = :name
                    """,
                    mapOf("name" to name0)
                ).use {
                    if (it.next()) {
                        it.getInt(0)
                    } else {
                        null
                    }
                }
                val nameIndex = maxIndex?.let { it + 1 } ?: 1
                user.name = name0
                user.nameIndex = nameIndex
                user.title = if (nameIndex > 1) "$name0 $nameIndex" else name0
            }
        }
        if (pic != null) {
            var dataUrl = pic.value
            if (dataUrl?.let { du -> user.pic?.let { du.endsWith(it) } } == true) {
                // no op
            } else if (dataUrl != null) {
                if (dataUrl.length > 2048) {
                    badParam("pic", "Слишком большая картинка")
                }
                if (!dataUrl.startsWith("data:image/jpeg;base64,")) {
                    badParam("pic", "Некорректная картинка")
                }
                dataUrl = dataUrl.substring(23)
                val img: BufferedImage
                val byteArray: ByteArray
                try {
                    byteArray = dataUrl.fromBase64(false)
                    img = ImageIO.read(ByteArrayInputStream(byteArray))
                } catch (t: Throwable) {
                    logWarning("Parse image error: ", t)
                    badParam("pic", "Некорректная картинка")
                }
                if (img.width != 48 || img.height != 48) {
                    badParam("pic", "Некорректный размер картинки")
                }
                val fileName = run {
                    val digest = MessageDigest.getInstance("MD5")
                    digest.update(byteArray)
                    val digestStr = digest.digest().toBase64(true)
                    val sb = StringBuilder(digestStr.length + 6)
                    sb.append(digestStr[0])
                    sb.append('/')
                    sb.append(digestStr[1])
                    sb.append('/')
                    sb.append(digestStr, 2, digestStr.length)
                    sb.append(".jpg")
                    sb.toString()
                }
                val file = File((System.getProperty("dots.uploadPath") ?: badConfig("!dots.uploadPath")) + fileName)
                if (!file.exists()) {
                    if (!file.parentFile.mkdirs()) {
                        badConfig("!mkdirs()")
                    }
                    file.writeBytes(byteArray)
                }
                System.getProperty("dots.uploadPath") + fileName
                user.pic = fileName
            } else {
                user.pic = null
            }
        }
        if (rules != null) {
            if (GRules.all.none { it.code == rules.value }) {
                badParam("rules", "Неизвестные правила игры")
            }
            user.rules = rules.value
        }
        db.save(user)
        user
    }
    threadContext.updateUser()
    Context.broadcastEvent(EvUser(GUser(user)))
}