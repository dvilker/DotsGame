package dotsgame.entities

import dotsgame.enums.UserLevel
import zDb.entities.annotations.Defaulted
import zDb.entities.annotations.EntityInfo

/**
 * Пользователи
 */
@EntityInfo([DEFAULT_SCHEMA])
interface User : ITitled {

    companion object: zDb.entities.EntityDescriptor<User>() {

        override fun Indexes.indexes() {
            indexes.add("CREATE UNIQUE INDEX :i ON :t USING BTREE (email) WHERE email IS NOT NULL")
            indexes.add("CREATE INDEX :i ON :t USING BTREE (name)")
//            indexes.add("CREATE UNIQUE INDEX :i ON :t USING BTREE (phone) WHERE phone IS NOT NULL")
        }

        override fun Defaults.defaults() {
            default(User::active, true)
            default(User::level, UserLevel.N)
            default(User::score, 1200)
            default(User::rules, "STANDARD")

            default(User::battleCount, 0)
            default(User::winCount, 0)
            default(User::winByTimeoutCount, 0)
            default(User::winByGroundCount, 0)
            default(User::winByFillCount, 0)
            default(User::winLineCount, 0)
            default(User::winWithStrongCount, 0)
            default(User::winWithStrongLineCount, 0)
            default(User::drawCount, 0)
            default(User::looseCount, 0)
            default(User::looseByTimeoutCount, 0)
            default(User::looseByFillCount, 0)
            default(User::looseByGroundCount, 0)
            default(User::looseLineCount, 0)
        }

        override fun Automations.automations() {
            constraint3("Укажите или email или phone", """ "email" IS NOT NULL OR "phone" IS NOT NULL """)
        }
    }

    @Defaulted
    var active: Boolean

    /**
     * Имя
     */
    var name: String

    /**
     * Номер, благодаря которому поддерживается уникальный title
     */
    var nameIndex: Int

    /**
     * Адрес E-Mail
     */
    var email: String?

    /**
     * Номер телефона
     */
    var phone: String?

    /**
     * Список групп безопасности
     */
    var roles: Set<Role>

    /**
     * Пароль
     */
    var password: String?

    /**
     * Токен авторизации
     */
    var token: String?

    /**
     * Время последнего действия с учетной записью (смена пароля, ...)
     */
    var lastActionTime: Long

    /**
     * Имя файла с картинкой
     */
    var pic: String?

    var level: UserLevel

    var score: Int

    /**
     * Число партий
     */
    var battleCount: Int

    /**
     * Побед всего
     */
    var winCount: Int

    /**
     * Побед по таймауту
     */
    var winByTimeoutCount: Int

    /**
     * Побед по заземлению
     */
    var winByGroundCount: Int

    /**
     * Побед по заполнению
     */
    var winByFillCount: Int

    /**
     * Число побед подряд
     */
    var winLineCount: Int

    /**
     * Число побед с более сильным игроком
     */
    var winWithStrongCount: Int

    /**
     * Число побед с более сильным игроком подряд
     */
    var winWithStrongLineCount: Int


    /**
     * Всего ничьих
     */
    var drawCount: Int

    /**
     * Всего поражений
     */
    var looseCount: Int
    var looseByTimeoutCount: Int
    var looseByGroundCount: Int
    var looseByFillCount: Int
    var looseLineCount: Int

    /**
     * Выбранные правила
     */
    var rules: String
}

fun User?.isSysAdmin(): Boolean = this != null && roles.isNotEmpty() && roles.any { it.isSysAdmin }