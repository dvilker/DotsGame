package dotsgame.server

import dotsgame.db.DbHandler
import dotsgame.db.updateDb
import dotsgame.entities.DEFAULT_SCHEMA
import dotsgame.robots.AbstractRobot
import zApi.api.dialogs.Captcha
import zDb.MAIN_DB
import zDb.postgres.PostgresUpdater
import zUtils.log.logDebug
import zUtils.log.logInfo
import zUtils.myjson.toJson
import java.io.StringWriter
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import kotlin.system.measureTimeMillis

class DotsListener: ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent) {
        DbHandler.initDbHandler()
        val updateSqlTime = measureTimeMillis {
            StringWriter().use { script ->
                for (masterConnectionString in DbHandler().getDbConnectionStrings(MAIN_DB, true)) {
                    PostgresUpdater.getConnection(masterConnectionString).use { connection ->
                        PostgresUpdater.getUpdateScript(connection, DEFAULT_SCHEMA, script, -1)
                        PostgresUpdater.update(connection, script.toString())
                    }
                }
            }
        }
        logDebug("updateSqlTime: {} ms", updateSqlTime)
        val updateDbTime = measureTimeMillis {
            updateDb()
        }
        logDebug("updateDbTime: {} ms", updateDbTime)
        Captcha.secret = appSecret
        logInfo("Запуск роботов...")
        AbstractRobot.startRobots()
        logInfo("Приложение запущено")
    }

    override fun contextDestroyed(sce: ServletContextEvent) {
        logInfo("Остановка роботов...")
        AbstractRobot.stopRobots()
    }
}