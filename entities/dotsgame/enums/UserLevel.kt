package dotsgame.enums

import zApi.api.ApiEnum
import zApi.api.ApiEnumExport
import zUtils.Title

@ApiEnum
enum class UserLevel(@ApiEnumExport val abb: Char) {
    @Title("Новичок")
    N('Н'),

    @Title("3 разряд")
    L3('3'),

    @Title("2 разряд")
    L2('2'),

    @Title("1 разряд")
    L1('1'),

    @Title("Кандидат")
    C('К'),

    @Title("Мастер")
    M('М'),

    @Title("Гроссмейстер")
    GM('Г'),

}