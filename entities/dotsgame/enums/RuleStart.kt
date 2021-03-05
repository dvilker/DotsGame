package dotsgame.enums

import zApi.api.ApiEnum
import zApi.api.ApiEnumExport

@ApiEnum
enum class RuleStart(
    @ApiEnumExport val title: String
) {
    EMPTY("Пустое поле"),
    CROSS("Скрест по-центру"),
    CROSS2("Два скреста по-центру"),
    CROSS4("Четыре скреста по-центру"),
    CROSS4R("Четыре скреста случайно"),
    RANDOM10("10 точек случайно"),
    RANDOM20("20 точек случайно"),

    ;
    companion object {
        val default = CROSS
    }
}