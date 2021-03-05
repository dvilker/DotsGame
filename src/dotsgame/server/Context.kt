package dotsgame.server

import dotsgame.api.*
import dotsgame.api.model.*
import dotsgame.entities.User
import dotsgame.entities.isUser
import org.eclipse.jetty.http.HttpCookie
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import zApi.api.context.CallContext
import zApi.api.context.HttpInfo
import zApi.api.context.invokeWithThreadCallContext
import zApi.api.context.withContext
import zApi.api.dialogs.setThreadDialogHandler
import zApi.api.servlet.ApiServlet
import zDb.*
import zDb.finder.eq
import zUtils.*
import zUtils.log.logInfo
import zUtils.log.logWarning
import zUtils.myjson.parseJson
import zUtils.myjson.toJson
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class Context private constructor(
    val user: User?,
    override val isDevil: Boolean,
    override val http: HttpInfo?,
    override val params: Map<String, Any?>,
) : CallContext, WebSocketListener {
    override val robotOrUserId: Long?
        get() = userId ?: robotId
    override val workplaceId: Long? = null
    override val pointId: Long? = null
    override var userId: GUserId? = user?.id
    override val robotId: Long? = null

    val name: String = "${connectionIndex.getAndIncrement()}#"
    val game = GConnection(this)


    override fun beforeCall() {
        super.beforeCall()
        setThreadDialogHandler(this)
    }

    override fun afterCall() {
        try {
            super.afterCall()
            setThreadDialogHandler(null)
        } finally {
            releaseAllDbs()
        }
    }

    /**
     * Копирование контекста, для дочерних потоков.
     *
     * Должен вызываться из самого потока
     */
    fun clone(): Context {
        return Context(user, isDevil, http, params)
    }

    object WsCreator : WebSocketCreator {
        override fun createWebSocket(req: ServletUpgradeRequest, resp: ServletUpgradeResponse): Context {
            return forWs(req, resp)
        }
    }

    // For sockets

    var closed = false
        private set

    @Volatile
    lateinit var session: Session

    val exUserOrNull = user?.let { GUser(it) }

    val exUser: GUser
        get() = exUserOrNull ?: badUnknownParam("Вы не авторизованы")

    val userIdNN: GUserId
        get() = userId ?: badUnknownParam("Вы не авторизованы")

    val userNN: User
        get() = user ?: badUnknownParam("Вы не авторизованы")

    private var isCalling = false

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        logInfo("Closed {0}: {1}, {2}", name, statusCode, reason)
        closing()
    }

    override fun onWebSocketConnect(session: Session) {
        this.session = session
        logInfo("Connected {0} from {1}", name, http?.remoteAddr)

        synchronized(connections) {
            connections.add(this)
            withContext {
                connected()
            }
            logInfo("Connections {0}", connections.size)
        }

    }

    override fun onWebSocketError(cause: Throwable?) {
        logInfo("Error {0}: {1}", name, cause)
        close(1006, "Error: $cause")
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        logInfo("Binary from {0}: {1}", name, payload.toHexString(offset, len))
    }

    override fun onWebSocketText(message: String) {
        if (message == ".") {
            session.remote.sendString(".", null)
            return
        }
        logInfo("Text from {0}: {1}", name, message)
        synchronized(this) {
            if (isCalling) {
                close(1002, "W-call")
                return
            }
            isCalling = true
        }
        try {
            try {
                @Suppress("UNCHECKED_CAST")
                val params = parseJson(message) as? Map<String, Any?> ?: badUnknownParam("Not object")
                val contextParams = this.params as MutableMap
                contextParams.clear()
                contextParams.putAll(params)
                val methodName = params["_"] as? String ?: badUnknownParam("No _")
                val method = ApiServlet.methods[methodName] ?: badUnknownParam("No method {0}", methodName)
                val result = mutableMapOf<String, Any?>()
                try {
                    result["result"] = this.invokeWithThreadCallContext(method) {
                        badAlgorithm("ApiDirect is not supported in ws")
                    }
                } catch (t: Throwable) {
                    ApiServlet.formatException(t.rootException(), result)
                }
                session.remote.sendString(
                    StringWriter().use {
                        it.append(' ')
                        result.toJson(it)
                        it.toString()
                    }
                )
            } catch (t: Throwable) {
                logWarning("Ошибка при обработке вызова метода {0}", t)
                close(1002, "Call err: $t")
            }
        } finally {
            synchronized(this) {
                isCalling = false
            }
        }
    }


    private fun closing() {
        if (!closed) {
            closed = true
            synchronized(connections) {
                connections.remove(this)
                logInfo("Connections {0}", connections.size)
            }
            withContext {
                disconnected()
            }
        }
    }

    fun dropConnection() {
        if (!closed) {
            closed = true
            close(4000, "Connect from other", false)
        }
    }

    private fun close(code: Int, reason: String?, doClosing: Boolean = true) {
        if (doClosing) {
            closing()
        }
        try {
            session.close(code, reason)
        } catch (e: Throwable) {
            logInfo("Error while close {0}: {1}", name, e)
        }
    }


    fun sendEvent(event: Ev) {
        session.remote.sendString(event.eventJson, null)
    }

    fun updateUser() {
        val user = user
        if (user != null) {
            getDb().load(
                user,
                User::title,
                User::name,
                User::email,
                User::phone,
                User::roles,
                User::token,
                User::pic,
                User::level,
                User::score,
                User::ruleSize,
                User::ruleStart,
                User::ruleTimer,
            )
        }
    }

    companion object {
        private var connectionIndex = AtomicLong(1)
        val connections = ConcurrentHashMap.newKeySet<Context>()

        inline fun broadcastEvent(event: Ev, toFilter: (Context) -> Boolean) {
            val message = event.eventJson
            synchronized(connections) {
                for (connection in connections) {
                    if (toFilter(connection)) {
                        connection.session.remote.sendString(message, null)
                    }
                }
            }
        }

        fun broadcastEvent(event: Ev) {
            val message = event.eventJson
            synchronized(connections) {
                for (connection in connections) {
                    connection.session.remote.sendString(message, null)
                }
            }
        }

        fun broadcastEventFor(targetUserId: GUserId, event: Ev) {
            val message = event.eventJson
            synchronized(connections) {
                for (connection in connections) {
                    if (connection.userId == targetUserId) {
                        connection.session.remote.sendString(message, null)
                    }
                }
            }
        }

        fun broadcastEventToBattle(battleId: GBattleId, event: Ev) {
            val message = event.eventJson
            synchronized(connections) {
                for (connection in connections) {
                    if (connection.game.room?.battleId == battleId) {
                        connection.session.remote.sendString(message, null)
                    }
                }
            }
        }

        fun broadcastEventToRoom(roomId: GRoomId, event: Ev) {
            val message = event.eventJson
            synchronized(connections) {
                for (connection in connections) {
                    if (connection.game.room?.id == roomId) {
                        connection.session.remote.sendString(message, null)
                    }
                }
            }
        }

        fun ServletUpgradeResponse.addCookie(cookie: Cookie) {
            require(!cookie.name.isNullOrBlank()) { "Cookie.name cannot be blank/null" }
            var comment: String? = cookie.comment
            val httpOnly = cookie.isHttpOnly() || HttpCookie.isHttpOnlyInComment(comment)
            val sameSite = HttpCookie.getSameSiteFromComment(comment)
            comment = HttpCookie.getCommentWithoutAttributes(comment)
            val httpCookie = HttpCookie(
                cookie.getName(),
                cookie.getValue(),
                cookie.getDomain(),
                cookie.getPath(),
                cookie.getMaxAge().toLong(),
                httpOnly,
                cookie.getSecure(),
                comment,
                cookie.getVersion(),
                sameSite
            )
            addHeader("Cookie", httpCookie.rfC6265SetCookie)
        }

        private fun ServletUpgradeResponse.addACookie(name: String, value: String?) {
            addCookie(Cookie(name, value).also { it.path = "/a/" })
        }

        private fun HttpServletResponse.addACookie(name: String, value: String?) {
            addCookie(Cookie(name, value).also { it.path = "/a/" })
        }

        private fun getUserByZCookie(zCookie: String): User? {
            if (zCookie.length < 22) {
                return null
            }
            val split = zCookie.split("z", limit = 2)
            if (split.size != 2) {
                return null
            }
            val id = split[0].toLongOrNull(35) ?: return null
            if (!isUser(id)) {
                return null
            }
            val user = getDb().getOrNull(
                id,
                User::active eq true,
                User::title,
                User::name,
                User::email,
                User::phone,
                User::roles,
                User::token,
                User::pic,
                User::level,
                User::score,
                User::ruleSize,
                User::ruleStart,
                User::ruleTimer,
            ) ?: return null
            if (user.token != split[1]) {
                return null
            }
            user.unsetValue(User::token)
            return user
        }

        fun getUserByLoginPassword(login: String, password: String, withToken: Boolean = false): User? {
            val user = getDb().find(
                User::active eq true,
                User::email eq login,
                User::title,
                User::name,
                User::email,
                User::phone,
                User::roles,
                User::password,
                User::pic,
                User::level,
                User::score,
                User::ruleSize,
                User::ruleStart,
                User::ruleTimer,
                if (withToken) User::token else null
            ).getOrNull() ?: return null
            if (user.password != getUserPasswordHash(user.id, password)) {
                return null
            }
            user.unsetValue(User::password)
            return user
        }

        fun forWs(req: ServletUpgradeRequest, resp: ServletUpgradeResponse): Context {
            val user = req.cookies?.firstOrNull { it.name == "z" && it.value != null }?.value?.let {
                getUserByZCookie(it)
            }
            return Context(
                user,
                false,
                object : HttpInfo {
                    override val remoteAddr: String = req.getHeader("x-forwarded-for") ?: req.remoteAddress
                    override fun getCookie(name: String) = badAlgorithm("No cookies in ws")
                    override fun setCookie(name: String, value: String?, maxAge: Int?) =
                        badAlgorithm("No cookies in ws")
                },
                mutableMapOf()
            )
        }

        fun forRequest(
            req: HttpServletRequest,
            resp: HttpServletResponse,
            allowBasicAuth: Boolean,
            params: Map<String, Any?>
        ): Context {
            val user = run getSafeUser@{
                req.cookies?.firstOrNull { it.name == "z" && it.value != null }?.value?.let {
                    val user = getUserByZCookie(it)
                    if (user != null) {
                        return@getSafeUser user
                    }
                }
                if (allowBasicAuth) {
                    val auth = req.getHeader("Authorization") ?: return@getSafeUser null
                    val matchResult = regex("(?i)basic\\s+([a-zA-Z0-9+/]+=*)").find(auth) ?: return@getSafeUser null
                    val loginPassword =
                        String(matchResult.groupValues[1].fromBase64(false), Charsets.UTF_8).split(
                            ':',
                            limit = 2
                        )
                    if (loginPassword.size != 2) {
                        return@getSafeUser null
                    }
                    val login = loginPassword[0]
                    val password = loginPassword[1]

                    return@getSafeUser getUserByLoginPassword(login, password)
                }
                null
            }
            return Context(
                user,
                false,
                object : HttpInfo {
                    override val remoteAddr: String
                        get() = req.getHeader("x-forwarded-for") ?: req.remoteAddr

                    override fun getCookie(name: String): String? {
                        return req.cookies?.firstOrNull { it.name == name && it.value != null }?.value
                    }

                    override fun setCookie(name: String, value: String?, maxAge: Int?) {
                        resp.addCookie(Cookie(name, value).also { cookie ->
                            cookie.path = "/a/"; cookie.isHttpOnly = true; maxAge?.let { cookie.maxAge = it }
                        })
                    }
                },
                params
            )
        }

        fun getUserPasswordHash(id: Long, password: String): String {
            return "a:" + "User $id $password $appSecret".sha256().toBase64(true)
        }

        /**
         * Функция устанавливает авторизационную куку
         */
        fun setUserCookie(http: HttpInfo, userId: Long, token: String) {
            http.setCookie("z", userId.toString(35) + "z" + token, null)
        }

        /**
         * Функция удаляет авторизационную куку
         */
        fun unsetUserCookie(http: HttpInfo) {
            http.setCookie("z", null, null)
        }
    }
}


val threadContextOrNull: Context? get() = CallContext.forThread.get() as? Context
val threadContext: Context get() = CallContext.forThread.get() as? Context ?: badAlgorithm("Контекст для потока не определен")
