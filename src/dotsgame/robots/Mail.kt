package dotsgame.robots

import java.net.URL
import java.util.*
import javax.activation.DataHandler
import javax.activation.URLDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


val mailSessionOrNull: Session? by lazy {
    if (System.getProperty("mail.mime.encodefilename") == null) {
        System.setProperty("mail.mime.encodefilename", "true")
    }
    val properties = System.getProperties()
    Session.getInstance(properties, object : Authenticator() {
        override fun getPasswordAuthentication() = PasswordAuthentication(properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"))
    })
}

fun parseInternetAddress(str: String): Array<InternetAddress> {
    val parsed = InternetAddress.parse(str)
    for(i in 0 until parsed.size) {
        parsed[i] = InternetAddress(parsed[i].address, parsed[i].personal, "utf-8")
    }
    return parsed
}


class MessageInfo(
    val message: MimeMessage,
    val multipart: MimeMultipart,
    val textPart: MimeBodyPart
) {
    private val contentAttachments = mutableMapOf<URL, String>()

    /**
     * На входе имя файла, а на выходе CID (ContentId)
     */
    fun addContentAttachment(file: URL): String {
        return contentAttachments.computeIfAbsent(file) { url ->
            val attachPart = MimeBodyPart()
            val id = "hatt-" + contentAttachments.size
            attachPart.contentID = id
            // Отладить изменение на url
            attachPart.dataHandler = DataHandler(URLDataSource(url)) // FileDataSource(path)
            multipart.addBodyPart(attachPart)
            id
        }
    }

    fun addContentAttachmentCid(file: URL): String {
        return "cid:" + addContentAttachment(file)
    }

    fun setHtml(html: String) {
        textPart.setText(html, "utf-8", "html")
    }

}

fun makeMultipartMessageOrNull(
    subject: String? = null,
    bodyText: String? = null,
    bodyType: String = "plain",
    to: String? = null,
    toTech: Boolean = false
): MessageInfo? {
    val mimeMessage = makeMimeMessageOrNull(
        subject = subject,
        to = to,
        toTech = toTech
    ) ?: return null

    val multipart = MimeMultipart()
    mimeMessage.setContent(multipart)

    val messageTextPart = MimeBodyPart()
    if (bodyText != null) {
        messageTextPart.setText(bodyText, "utf-8", bodyType)
    }
    multipart.addBodyPart(messageTextPart)
    return MessageInfo(mimeMessage, multipart, messageTextPart)
}


fun makeMimeMessageOrNull(
    subject: String? = null,
    bodyText: String? = null,
    bodyType: String = "plain",
    to: String? = null,
    toTech: Boolean = false
): MimeMessage? {
    val session = mailSessionOrNull ?: return null
    val message = MimeMessage(session)

    val from = session.properties.getProperty("mail.app.mailFrom")
    if (!from.isNullOrBlank()) {
        val parsedFrom = parseInternetAddress(from)
        if (parsedFrom.isNotEmpty()) {
            message.setFrom(parsedFrom[0])
        }
    }
    message.sentDate = Date()
    if (subject != null) {
        message.subject = subject
    }
    if (bodyText != null) {
        message.setText(bodyText, "utf-8", bodyType)
    }

    run {
        val mailBccAddresses = session.properties.getProperty("mail.app.mailBccAddresses")
        if (!mailBccAddresses.isNullOrBlank()) {
            message.addRecipients(
                Message.RecipientType.BCC,
                parseInternetAddress(mailBccAddresses)
            )
        }
    }

    if (toTech) {
        val mailTechAddresses = session.properties.getProperty("mail.app.mailTechAddresses")
        if (!mailTechAddresses.isNullOrBlank()) {
            message.addRecipients(
                Message.RecipientType.TO,
                parseInternetAddress(mailTechAddresses)
            )
        }
    }

    if (!to.isNullOrBlank()) {
        message.addRecipients(
            Message.RecipientType.TO,
            parseInternetAddress(to)
        )
    }
    return message
}

fun Message.sendImmediately() {
    Transport.send(this)
}

fun MessageInfo.sendImmediately() {
    Transport.send(this.message)
}

/**
 * Заглушка для отправки в очереди
 */
fun Message.send() {
    Transport.send(this)
}

/**
 * Заглушка для отправки в очереди
 */
fun MessageInfo.send() {
    Transport.send(this.message)
}