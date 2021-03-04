package dotsgame.entities

import zDb.DB_NOW
import zDb.entities.DbQueue
import zDb.entities.Entity
import zDb.entities.annotations.Defaulted
import zDb.entities.annotations.EntityInfo
import java.time.LocalDateTime

/**
 * Пользователи
 */
@EntityInfo([DEFAULT_SCHEMA], allowDelete = true)
interface Verification : DbQueue {

    companion object: zDb.entities.EntityDescriptor<Verification>() {

        override fun Indexes.indexes() {
            indexes.add("CREATE UNIQUE INDEX :i ON :t USING BTREE (target) WHERE target IS NOT NULL")
            indexes.add("CREATE UNIQUE INDEX :i ON :t USING BTREE (sent_time) WHERE sent_time IS NULL")
        }

        override fun Defaults.defaults() {
            default(Verification::addTime, DB_NOW)
            default(Verification::tryCount, 3)
        }
    }

    /**
     * Телефон или e-mail
     */
    var target: String

    /**
     * Отправленный код
     */
    var code: String

    /**
     * Осталось попыток проверить код
     */
    @Defaulted
    var tryCount: Int

    @Defaulted
    var addTime: LocalDateTime

    var sentTime: LocalDateTime?

    var used: LocalDateTime?

    var sentError: String?


}
