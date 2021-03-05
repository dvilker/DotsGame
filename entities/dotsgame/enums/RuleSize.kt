package dotsgame.enums

import zApi.api.ApiEnum
import zApi.api.ApiEnumExport

@ApiEnum
enum class RuleSize(
    @ApiEnumExport val title: String,
    @ApiEnumExport val width: Int,
    @ApiEnumExport val height: Int
) {
    STANDARD("Стандарт", 39, 32),
    MINI("Мини", 16, 16),

    ;
    companion object {
        val default = STANDARD
    }
}