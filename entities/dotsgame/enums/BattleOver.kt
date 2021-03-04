package dotsgame.enums

import zApi.api.ApiEnum
import zUtils.Title

@ApiEnum
enum class BattleOver() {

    @Title("Кончились ходы")
    FULL_FILL,

    @Title("Игрок сдался")
    SURRENDER,

    @Title("Таймаут")
    TIMEOUT,

    @Title("Ничья")
    DRAW,

    @Title("Заземление")
    GROUND,
}