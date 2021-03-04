/*
 * Все права защищены.
 * Автор: Дмитрий Вилькер, 2020-2021
 */
@file:JvmName("Main")

import dotsgame.server.Context
import dotsgame.server.DotsListener
import dotsgame.server.DotsApiServlet
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.server.*
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.component.LifeCycle
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.log.Logger
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter
import zUtils.log.*
import zUtils.log.hooks.FilesHook
import java.io.File
import java.util.*
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    setLogMarker("Main")

    if (args.size != 1) {
        logError("Параметры: <путь к файлу конфигурации>")
        exitProcess(1)
    }
    try {
        Properties().also { config ->
            File(args[0]).reader().use {
                config.load(it)
                System.getProperties().putAll(config)
            }
        }
    } catch (t: Throwable) {
        logError("Ошибка чтения конфигурации: {0}", t)
        exitProcess(1)
    }

    val httpPort = System.getProperty("dots.port")?.toIntOrNull()
    if (httpPort == null || httpPort !in 1..65535) {
        logError("Порт не является числом, либо вне диапазона 1..65535.")
        exitProcess(1)
    }

    val logPathTemplate = System.getProperty("log.path.template")
    if (logPathTemplate != null) {
        val currentSymlink = System.getProperty("log.path.currentSymlink")
        if (currentSymlink != null) {
            logHint("Шаблон для логов: {0}, ссылка на текущий лог: {1}", logPathTemplate, currentSymlink)
            addLogHook(FilesHook.forTemplate(logPathTemplate, currentSymlink))
            logHint("Шаблон для логов: {0}, ссылка на текущий лог: {1}", logPathTemplate, currentSymlink)
        } else {
            logHint("Шаблон для логов: {0}", logPathTemplate)
            addLogHook(FilesHook.forTemplate(logPathTemplate))
            logHint("Шаблон для логов: {0}", logPathTemplate)
        }
        logHint("Файл конфигурации: {0}", args[0])
    } else {
        addLogHook(defaultLogHook)
    }

    try {
        logHint("Инициализация веб-сервера на порту {0}...", httpPort)

        class JettyLogger(val loggerName: String): Logger {
            private var debug = false
            private var marker = LogMarker(loggerName)
            override fun warn(msg: String, vararg args: Any?) = logMessage(LogType.WARNING, msg, *args, marker = marker)
            override fun warn(thrown: Throwable?) = logMessage(LogType.WARNING, "", thrown, marker = marker)
            override fun warn(msg: String, thrown: Throwable?) = logMessage(LogType.WARNING, msg, thrown, marker = marker)
            override fun getName(): String = loggerName
            override fun toString(): String = "$loggerName Logger"
            override fun info(msg: String, vararg args: Any?) = logMessage(LogType.HINT, msg, *args, marker = marker)
            override fun info(thrown: Throwable?) = logMessage(LogType.HINT, "", thrown, marker = marker)
            override fun info(msg: String, thrown: Throwable?) = logMessage(LogType.HINT, msg, thrown, marker = marker)
            override fun getLogger(name: String): Logger = JettyLogger(name)
            override fun setDebugEnabled(enabled: Boolean) { debug = enabled }
            override fun ignore(ignored: Throwable?) {} // = logHint("", ignored)
            override fun isDebugEnabled(): Boolean = debug
            override fun debug(msg: String, vararg args: Any?) { if (debug) logMessage(LogType.DEBUG, msg, *args, marker = marker) }
            override fun debug(msg: String, value: Long) { if (debug) logMessage(LogType.DEBUG, msg, value, marker = marker) }
            override fun debug(thrown: Throwable?) { if (debug) logMessage(LogType.DEBUG, "", thrown, marker = marker) }
            override fun debug(msg: String, thrown: Throwable?) { if (debug) logMessage(LogType.DEBUG, msg, thrown, marker = marker) }
        }
        Log.setLog(JettyLogger("Jetty"))

        val server = Server(httpPort)
        server.stopAtShutdown = true
        server.errorHandler = ErrorHandler()
        for (connector in server.connectors) {
            for (connectionFactory in connector.connectionFactories) {
                if (connectionFactory is HttpConnectionFactory) {
                    connectionFactory.httpConfiguration.sendServerVersion = false
                }
            }
        }
        server.handler = ServletContextHandler(0).also { context ->
            context.contextPath = "/"
            context.baseResource = Resource.newClassPathResource("web")
            context.welcomeFiles = arrayOf("index.html", "index.htm")
            context.mimeTypes.apply {
                addMimeMapping("htm", "text/html; charset=utf-8")
                addMimeMapping("html", "text/html; charset=utf-8")
                addMimeMapping("txt", "text/plain; charset=utf-8")
            }
            context.addLifeCycleListener(object :
                org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener() {
                override fun lifeCycleStarting(event: LifeCycle) {
                    context.servletContext.isExtendedListenerTypes = true
                    context.servletContext.addListener(DotsListener())
                    ServletHolder(DotsApiServlet()).let {
                        it.registration.setMultipartConfig(MultipartConfigElement(""))
                        it.registration.setLoadOnStartup(1)
                        context.addServlet(it, "/a/*")
                    }
                    NativeWebSocketServletContainerInitializer.configure(context) { _, webSocketConfiguration ->
                        webSocketConfiguration.policy.maxTextMessageBufferSize = 65535
                        webSocketConfiguration.policy.maxBinaryMessageSize = 0
                        webSocketConfiguration.policy.idleTimeout = 40000
                        webSocketConfiguration.addMapping("/a/s/", Context.WsCreator)
                    }
                    WebSocketUpgradeFilter.configure(context)
                    val uploadPath = System.getProperty("dots.uploadPath")
                    if (uploadPath != null) {
                        ServletHolder(DefaultServlet()).let {
                            it.setInitParameter("resourceBase", uploadPath)
                            it.setInitParameter("dirAllowed", "false")
                            it.setInitParameter("pathInfoOnly", "true")
                            it.setInitParameter("welcomeServlets", "false")
                            val uploadUri = System.getProperty("dots.uploadUri")
                            val pathSpec = when {
                                uploadUri.isNullOrEmpty() -> "/up/*"
                                uploadUri.endsWith("/") -> "$uploadUri*"
                                else -> "$uploadUri/*"
                            }
                            logInfo("Веб-монтирование {0} -> {1}", uploadPath, pathSpec)
                            context.addServlet(it, pathSpec)
                        }
                    } else {
                        logWarning("Веб-монтирование папки /up/ пропущено, т.к. не задан параметр dots.uploadPath")
                    }
                    ServletHolder(DefaultServlet()).let {
                        it.setInitParameter("precompressed", "true")
                        it.setInitParameter("dirAllowed", "false")
                        context.addServlet(it, "/")
                    }
                }
            })
        }
        logInfo("Запуск веб-сервера на порту {0}...", httpPort)
        server.start()
        logInfo("Веб-сервер запущен.")
        server.join()
        logInfo("Веб-сервер остановлен.")
    } catch (t: Throwable) {
        logError("Ошибка веб-сервера: {0}", t)
        exitProcess(1)
    }
    exitProcess(0)
}

class ErrorHandler : org.eclipse.jetty.server.handler.ErrorHandler() {
    override fun handle(
        target: String?,
        baseRequest: Request,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        response.contentType = MimeTypes.Type.TEXT_PLAIN_UTF_8.asString()
        response.characterEncoding = MimeTypes.Type.TEXT_PLAIN_UTF_8.charset.name()
        response.writer.use { writer ->
            writer.println(baseRequest.getAttribute(Dispatcher.ERROR_STATUS_CODE))
            writer.println(baseRequest.getAttribute(Dispatcher.ERROR_MESSAGE))
            (baseRequest.getAttribute(Dispatcher.ERROR_EXCEPTION) as Throwable).printStackTrace(writer)
        }
    }
}
