package dotsgame.enums

import zApi.api.ApiEnum
import zApi.api.ApiEnumExport

@ApiEnum
enum class RuleTimer(
    @ApiEnumExport val title: String,
    @ApiEnumExport val fullTime: Int,
    @ApiEnumExport val moveTime: Int,
    @ApiEnumExport val addUnused: Boolean,
    @ApiEnumExport val randomMove: Boolean
) {
    STANDARD("Стандарт", 4 * 60, 20, false, false),
    FISHER("Фишер", 4 * 60, 20, true, false),
    BLITZ("Блиц", 60, 5, false, false),
    BLITZ_RANDOM("Блиц со случайным ходом", 0, 5, false, true),

    ;
    companion object {
        val default = STANDARD
    }
}