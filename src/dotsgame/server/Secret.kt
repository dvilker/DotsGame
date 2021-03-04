package dotsgame.server

import dotsgame.entities.Common
import zDb.connectDb
import zDb.get
import zUtils.log.logError
import zUtils.log.logHint

val appCommon: Common by lazy {
    try {
        connectDb().use { db ->
            val com = db.get(
                Common::icon,
                Common::label,
                Common::secret,
            )
            logHint(
                "База: {0}; Секрет базы: {2}...",
                com.label,
                com.secret.substring(0, 3)
            )
            com
        }
    } catch (t: Throwable) {
        logError("Не удалось получить Common: {0}", t)
        throw t
    }
}

/**
 * Уникальное значение для базы. 32 символа A-Za-z0-9
 */
val appSecret: String = appCommon.secret

val appSecretBytes = appSecret.toByteArray()