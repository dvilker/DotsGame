package dotsgame.enums

import zApi.api.ApiEnum
import zApi.api.ApiEnumExport

@ApiEnum
enum class RuleTimer(
    @ApiEnumExport val title: String,
    @ApiEnumExport val fullTime: Int,
    @ApiEnumExport val moveTime: Int,
    @ApiEnumExport val addUnused: Boolean
) {
    STANDARD("Стандарт", 4 * 60, 20, false),
    FISHER("Фишер", 4 * 60, 20, true),
    BLITZ("Блиц", 60, 5, false)

    ;
    companion object {
        val default = STANDARD
    }
}