package dotsgame.db

import dotsgame.entities.*
import dotsgame.server.Context
import zDb.*
import zDb.entities.Entity
import zDb.entities.EntityEvents
import zUtils.CodeChars
import zUtils.badAlgorithm
import zUtils.generateCode
import zUtils.log.logBase
import zUtils.log.logHint
import java.security.SecureRandom
import kotlin.random.asKotlinRandom
import kotlin.system.measureTimeMillis

/**
 * Логические обновлялки базы. Нужно внимательно следить за версиями.
 */
private fun getDbUpdates(): List<DbUpdate> = listOf(
    DbUpdate(1, "common") { db ->
        val random = SecureRandom()
        val secret = generateCode(
            32,
            setOf(CodeChars.DIGITS, CodeChars.LOWER_AZ, CodeChars.UPPER_AZ),
            random.asKotlinRandom()
        )
        val values = mutableMapOf<String, Any?>(
            "secret" to secret
        )

        @Suppress("SqlResolve")
        db.execute(
            """
                UPDATE common 
                SET secret = CASE WHEN secret IS NULL OR secret = '' THEN :secret ELSE secret END, 
                    "label" = /*if label*/ :label /*fi*//*if !label*/ DEFAULT /*fi*/, 
                    "icon" = /*if icon*/ :icon /*fi*//*if !icon*/ DEFAULT /*fi*/
            """,
            values
        )
        commitTransactionDbAndBegin()
    },
    DbUpdate(2, "User init") { db ->
        val sysAdminRole = newRole(
            title = "Системный администратор",
            isSysAdmin = true
        )
        db.save(sysAdminRole)
        val user = newUser(
            title = "Системный администратор",
            name = "Системный администратор",
            nameIndex = 1,
            email = "1@vilker.ru",
            roles = setOf(sysAdminRole),
            lastActionTime = System.currentTimeMillis()
        )
        db.acquireId(user)
        user.password = Context.getUserPasswordHash(user.id, "admax11")
        db.save(user)
    },
)


private fun pauseChecks(db: Db): String {
    db.query(
        """
        SELECT
            string_agg('ALTER TABLE "'||cls.relname||'" DROP CONSTRAINT "'||con.conname||'"', '; ') drp,
            string_agg('ALTER TABLE "'||cls.relname||'" ADD CONSTRAINT "'||con.conname||'" '|| pg_get_constraintdef(con.oid), '; ') crt
        FROM pg_constraint con
            JOIN pg_class cls ON con.conrelid=cls.oid
            JOIN pg_namespace nsp ON nsp.oid=cls.relnamespace
        WHERE con.contype = 'c'
          AND nsp.nspname = 'public'
          AND (con.conname LIKE '%___notnull' OR con.conname LIKE '_ac_%')
        HAVING count(*) > 0
        """
    ).use { ds ->
        return if (ds.next()) {
            logBase("Временно удаляем все ограничения...")
            db.execute(ds.getString(0) ?: badAlgorithm())
            ds.getString(1) ?: ""
        } else {
            ""
        }
    }
}

fun updateDb() {
    try {
        getDb().transactionDb { db ->
            val common = db.get(Common::version)
            var version = common.version

            val dbUpdates = getDbUpdates()

            for (i in 1 until dbUpdates.size) {
                if (dbUpdates[i-1].version >= dbUpdates[i].version) {
                    badAlgorithm("dbUpdates должен быть упорядочен по version")
                }
            }

            var pausedChecks: String? = null

            for (dbUpdate in dbUpdates) {
                if (version < dbUpdate.version) {
                    if (pausedChecks == null) {
                        pausedChecks = pauseChecks(db)
                    }
                    version = dbUpdate.version
                    logBase("Логическое обновление до версии {0}: {1}...", dbUpdate.version, dbUpdate.title)
                    logBase(
                        "Логическое обновление до версии {0} заняло {1} мс",
                        dbUpdate.version,
                        measureTimeMillis {
                            dbUpdate.code(this, db)
                        }
                    )
                }
            }

            if (!pausedChecks.isNullOrBlank()) {
                logBase("Восстанавливаем удаленные ограничения...")
                db.execute(pausedChecks)
            }

            if (version != common.version) {
                common.version = version
                db.save(common)
                logHint("Логические обновления завершены. Установлена версия: {0}", version)
            } else {
                logHint("Логические обновления не потребовались, версия: {0}", version)
            }
        }
    } finally {
        releaseAllDbs()
    }
}

private class DbUpdate(val version: Int, val title: String, val code: TransactionHandler.(db: Db) -> Unit)

private inline fun timeLog(title: String = "Запрос", code: () -> Unit) {
    logBase("{0}...", title)
    logBase(
        "Готово за {0} мс.",
        measureTimeMillis {
            code()
        }
    )
}

private inline fun timeAndCountLog(title: String = "Запрос", code: () -> Int) {
    logBase("{0}...", title)
    val count: Int
    val time = measureTimeMillis {
        count = code()
    }
    logBase("Готово за {0} мс: {1} шт.", time, count)
}

class EntityEvents: EntityEvents {
//    override fun afterSave(e: Entity, db: Db) {
//        if (isUser(e.id) || isRole(e.id)) {
//            db.doAfterCommit {
//                ApiCache.clearFor(e.id)
//            }
//        }
//    }
}