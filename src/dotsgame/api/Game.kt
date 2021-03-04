package dotsgame.api

import dotsgame.api.model.GRules
import zApi.api.ApiMethod


@ApiMethod
fun getRules(): List<GRules> = GRules.all

//
//import dotsgame.api.model.GUser
//import zApi.api.ApiMethod
//import zUtils.ZException
//import zUtils.badAlgorithm
//import zUtils.myjson.JsonTransient
//import kotlin.experimental.and
//import kotlin.experimental.inv
//import kotlin.experimental.or
//
//
//
//enum class GameBattleState {
//    OFFER,
//    SIDE1,
//    SIDE2,
//    OVER_FULL_FILL,
//    OVER_SURRENDER,
//    OVER_TIMEOUT,
//    OVER_DRAW,
//}
//
//class GameRules (
//    val title: String,
//    val width: Int,
//    val height: Int,
//    val moveTime: Int,
//    val totalTime: Int,
//) {
//    companion object {
//        val STANDARD = GameRules(
//            "Стандартные",
//            39, 32,
//            20,
//            4 * 60
//        )
//    }
//}
//
//class GameSide(
//    val user: GUser,
//    var captured: Int = 0
//)
//
//class GameBattle (
//    val id: Long,
//    var state: GameBattleState = GameBattleState.OFFER,
//    var sides: List<GameSide>,
//    var rules: GameRules = GameRules.STANDARD,
//    /**
//     * Ходы по очереди каждой стороны.
//     *
//     * Каждый ход - два символа:
//     *  - закодированная горизонтальная координата
//     *  - .. вертикальная ..
//     *
//     *  Закодированное число - число в системе счисления 62 с алфавитом 0-9 a-z A-Z
//     */
//    @JsonTransient
//    var moves: StringBuilder = StringBuilder(),
//    var winSide: Int? = null
//) {
//    init {
//        if (sides.size != 2) {
//            badAlgorithm("Need exact 2 sides")
//        }
//    }
//    fun addMove(x: Int, y: Int) {
//        val field = getField()
//        field.addMove(x, y) // for check
//        moves.append(x.to62())
//        moves.append(y.to62())
//        field.recountCaptured()
//    }
//
//    fun getField(): GameField {
//        val field = GameField(rules.width, rules.height, sides)
//        for(i in moves.indices step 2) {
//            val x = moves[i].from62()
//            val y = moves[i+1].from62()
//            field.addMove(x, y)
//        }
//        return field
//    }
//}
//
//fun Int.to62(): Char {
//    return when(this) {
//        in 0..9 ->  '0' + this
//        in 10..35 ->  'a' + this - 10
//        in 36..61 ->  'A' + this - 36
//        else -> badAlgorithm()
//    }
//}
//
//fun Char.from62(): Int {
//    return when(this) {
//        in '0'..'9' -> this - '0'
//        in 'a'..'z' -> this - 'a' + 10
//        in 'A'..'Z' -> this - 'A' + 36
//        else -> badAlgorithm()
//    }
//}
//
//
//private const val SIDE: Byte =     0b0011
//private const val CAP_SIDE: Byte = 0b1100
//private const val F: Byte =     0b01_0000
//private const val N: Byte =     0b10_0000
//private const val FN: Byte =    0b11_0000
//class GameField(val width: Int, val height: Int, val sides: List<GameSide>) {
//    private fun Byte.side(): Int = (this and SIDE).toInt()
//    private fun Byte.capturedSide(): Int = (this and CAP_SIDE).toInt() shr 2
//    private fun Byte.flooded(): Boolean = this and F > 0
//    private fun Byte.floodedNow(): Boolean = this and N > 0
//
//    /**
//     * Каждый байт это:
//     * 76543210
//     *       SS - side
//     *     CC   - captured side
//     *    F     - Уже искали
//     *   N      - Нашли только что
//     */
//    private val field = ByteArray(width * height)
//    var moveCount: Int = 0
//        private set
//
//    private fun index(x: Int, y: Int): Int = x * height + y
//
//    fun addMove(x: Int, y: Int) {
//        if (x < 0 || x >= width || y < 0 || y >= height) {
//            throw IndexOutOfBoundsException()
//        }
//        var side = moveCount % sides.size
//        val index = index(x, y)
//        if (field[index] != 0.toByte()) {
//            throw BadGame("Ход в занятую позицию")
//        }
//        field[index] = side.toByte()
//        checkCaptures(side)
//        for(i in 1 until sides.size) {
//            side ++
//            if (side > sides.size) {
//                side = 0
//            }
//            checkCaptures(side)
//        }
//        moveCount++
//    }
//
//    private fun checkCaptures(side: Int) {
//        /**
//         * Находим все чужие точки не с краю
//         */
//        for(x in 1 until (width - 1)) {
//            for(y in 1 until (height - 1)) {
//                val dot = field[index(x, y)]
//                if (dot.side() == side || dot.capturedSide() != 0 || dot.flooded()) {
//                    continue
//                }
//                val captured: Byte = if (!flood(x, y, side)) (side shl 2).toByte() else 0
//                for(x1 in 1 until (width - 1)) {
//                    for (y1 in 1 until (height - 1)) {
//                        val index = index(x1, y1)
//                        if (field[index].floodedNow()) {
//                            field[index] = field[index] or captured and N.inv()
//                        }
//                    }
//                }
//            }
//        }
//        for(x1 in 1 until (width - 1)) {
//            for (y1 in 1 until (height - 1)) {
//                val index = index(x1, y1)
//                field[index] = field[index] and F.inv()
//            }
//        }
//    }
//
//    /**
//     * Возвращает true - если достигли стенки
//     */
//    private fun flood(x: Int, y: Int, side: Int): Boolean {
//        if (x < 0 || x > width || y < 0 || y > height) {
//            return true
//        }
//        val index = index(x, y)
//        val dot = field[index]
//        if (dot.side() == side || dot.flooded()) {
//            return false
//        }
//        field[index] = field[index] or FN
//        return flood(x + 1, y, side) || flood(x - 1, y, side) || flood(x, y + 1, side) || flood(x, y - 1, side)
//    }
//
//    fun recountCaptured() {
//        sides.forEach { it.captured = 0 }
//        for(x in 1 until (width - 1)) {
//            for(y in 1 until (height - 1)) {
//                val dot = field[index(x, y)]
//                val side = dot.side()
//                val capturedSide = dot.capturedSide()
//                if (capturedSide != 0 && capturedSide != side) {
//                    sides[capturedSide].captured ++
//                }
//            }
//        }
//    }
//}
//
//class BadGame : ZException {
//    constructor(message: String) : super("", message)
//    constructor(message: String, vararg params: Any?) : super("", message, *params)
//}
