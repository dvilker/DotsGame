package dotsgame.api.model

data class GRules (
    val code: String,
    val title: String,
    val width: Int,
    val height: Int,
    val moveTime: Int,
    val totalTime: Int,
) {

    enum class GRulesSize(val title: String, val width: Int, height: Int) {
        STANDARD("Стандарт", 39, 32),
        MINI("Мини", 15, 15),
    }
    enum class GRulesTime(val title: String, val fullTime: Int, val moveTime: Int, addUnused: Boolean, randomMove: Boolean) {
        STANDARD("Стандарт", 4 * 60, 20, false, false),
        FISHER("Фишер", 4 * 60, 20, true, false),
        BLITZ("Блиц", 60, 5, false, false),
        BLITZ_RANDOM("Блиц со случайным ходом", 0, 5, false, true),
    }
    enum class GRulesStart(val title: String) {
        EMPTY("С пустого поля"),
        CROSS("Скрест по-центру"),
        CROSS2("Два скреста по-центру"),
        CROSS4("Четыре скреста по-центру"),
        CROSS4R("Четыре скреста случайно"),
        RANDOM10("10 точек случайно"),
        RANDOM20("20 точек случайно"),
    }





    companion object {
        val default get() = all[0]

        val all = listOf(
            GRules(
                "STANDARD",
                "Стандартные",
                39, 32,
                20,
                4 * 60
            ),
            GRules(
                "BLITZ",
                "Блиц",
                39, 32,
                5,
                60
            ),
            GRules(
                "MINI",
                "Мини",
                15, 15,
                20,
                4 * 60
            ),
            GRules(
                "MINI_BLITZ",
                "Мини блиц",
                15, 15,
                5,
                60
            )
        )
    }

}
