package dotsgame.entities

import zDb.entities.SingletonEntity
import zDb.entities.annotations.EntityInfo
import zDb.entities.annotations.LegacyTransient

/**
 * Общие параметры
 */
@EntityInfo([DEFAULT_SCHEMA], singleton = true)
@LegacyTransient
interface Common : SingletonEntity {

    companion object : zDb.entities.EntityDescriptor<Common>() {
        override fun Defaults.defaults() {
            default(Common::version, 0)
            default(Common::secret, "")
            default(Common::label, "Точки")
            default(Common::icon, "∗ #000")
        }
    }

    /**
     * Версия базы для логических обновлений
     */
    var version: Int

    /**
     * Секретное значение, состоящие из букв и цифр.
     *
     * Используется в качестве соли в хешах.
     */
    var secret: String

    /**
     * Название базы.
     *
     * Например: Гамма
     */
    var label: String

    /**
     * Иконка базы в формате: <Символ> <Цвет фона>.
     *
     * Например: SP #330
     */
    var icon: String

}
