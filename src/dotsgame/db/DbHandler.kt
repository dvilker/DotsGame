package dotsgame.db

import zDb.DB_HANDLER_PROPERTY
import zDb.MAIN_DB
import zUtils.badAlgorithm
import zUtils.badConfig
import zUtils.nullIfBlank

class DbHandler: zDb.DbHandler {
    override fun getDbConnectionStrings(dbName: String, master: Boolean): List<String> {
        if (dbName == MAIN_DB) {
            if (master) {
                val prop = System.getProperty("dots.dbWrite")
                if (prop.isNullOrBlank()) {
                    badConfig("Файл конфигурации не содержит параметра dots.dbWrite")
                }
                return prop.trim().split(Regex("\\s+"))
            } else {
                val prop = System.getProperty("dots.dbReadOnly").nullIfBlank() ?: System.getProperty("dots.dbWrite")
                if (prop.isNullOrBlank()) {
                    return getDbConnectionStrings(dbName, true)
                }
                return prop.trim().split(Regex("\\s+"))
            }
        } else {
            badAlgorithm()
        }
    }

    override fun getThreadUserId(): Long? {
        return null
    }

    companion object {
        fun initDbHandler() {
            System.setProperty(DB_HANDLER_PROPERTY, DbHandler::class.java.name)
        }
    }
}
