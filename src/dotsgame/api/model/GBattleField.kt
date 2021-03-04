package dotsgame.api.model

import zUtils.badAlgorithm
import zUtils.badUnknownParam
import zUtils.log.logDebug
import kotlin.experimental.and
import kotlin.experimental.or


fun Int.to62(): Char {
    return when(this) {
        in 0..9 ->  '0' + this
        in 10..35 ->  'a' + this - 10
        in 36..61 ->  'A' + this - 36
        else -> badAlgorithm()
    }
}

fun Char.from62(): Int {
    return when(this) {
        in '0'..'9' -> this - '0'
        in 'a'..'z' -> this - 'a' + 10
        in 'A'..'Z' -> this - 'A' + 36
        else -> badAlgorithm()
    }
}


private const val SIDE: Byte =              0b000_0011
private const val CAP_SIDE: Byte =          0b000_1100
private const val CAP_SIDE_NEG: Byte =      0b111_0011
private const val F: Byte =                 0b001_0000
private const val N: Byte =                 0b010_0000
private const val N_NEG: Byte =             0b101_1111
private const val N_AND_CAP_SIDE_NEG: Byte =0b101_0011
private const val FN: Byte =                0b011_0000
private const val FN_NEG: Byte =            0b100_1111
private const val GND: Byte =               0b100_0000
private const val FREE: Byte =              0

class GBattleField(val width: Int, val height: Int) {
    private fun Byte.side(): Int = (this and SIDE).toInt() - 1
    private fun Byte.capturedSide(): Int = ((this and CAP_SIDE).toInt() shr 2) - 1
    private fun Byte.flooded(): Boolean = this and F > 0
    private fun Byte.floodedNow(): Boolean = this and N > 0
    private fun Byte.grounded(): Boolean = this and GND > 0

    /**
     * Каждый байт это:
     * 76543210
     *       SS - side
     *     CC   - captured side
     *    F     - Уже искали
     *   N      - Нашли только что
     */
    private val field = ByteArray(width * height)
    var moveSide: Int = 0
        private set

    var moveCount: Int = 0
        private set

    var score0: Int = 0
        private set

    var score1: Int = 0
        private set

    private fun index(x: Int, y: Int): Int = x * height + y

    fun addMove(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            badUnknownParam("Ход вне поля")
        }
        val index = index(x, y)
        if (field[index] != FREE) {
            badUnknownParam("Ход в занятую позицию")
        }
        field[index] = (moveSide + 1).toByte()
        var captured = if (checkCaptures(moveSide)) 1 else 0
        ground(moveSide)
        moveSide = 1 - moveSide
        captured += if (checkCaptures(moveSide)) 2 else 0
        ground(moveSide)
        if (captured > 0) {
            countCaptured()
        }
        moveCount++
//        dump()
        logDebug("groundScore({0}): {1}", moveSide, groundScore(moveSide))
        logDebug("groundScore({0}): {1}", 1 - moveSide, groundScore(1 - moveSide))
        return captured
    }

    private fun colorDump(): String {
        val RESET = "\u001B[0m"
        val FG_RED = "\u001B[91m"
        val FG_DARK_RED = "\u001B[31m"
        val BG_RED = "\u001B[41m"
        val FG_BLUE = "\u001B[94m"
        val FG_DARK_BLUE = "\u001B[34m"
        val BG_BLUE = "\u001B[44m"

        val sb = StringBuilder()

        sb.append(' ').append(' ')
        for (x in 0 until width) {
            sb.append(' ').append(x / 10)
        }
        sb.appendLine().append(' ').append(' ')
        for (x in 0 until width) {
            sb.append(' ').append(x % 10)
        }
        sb.appendLine()
        for(y in (0 until height).reversed()) {
            sb.append(y.toString().padStart(2))
            for (x in 0 until width) {
                sb.append(RESET)
                sb.append(' ')
                val dot = field[index(x, y)]
                val side = dot.side()
                if (dot.flooded()) {
                    sb.append('ᷫ')
                }
                if (dot.floodedNow()) {
                    sb.append('ᷠ')
                }
                val capturedSide = dot.capturedSide()
                when (capturedSide) {
                    -1 -> {}
                    0 -> sb.append(BG_BLUE)
                    1 -> sb.append(BG_RED)
                    else -> badAlgorithm()
                }
                when (side) {
                    -1 -> sb.append('·')
                    0 -> sb.append(FG_BLUE).append('●')
                    1 -> sb.append(FG_RED).append('●')
                    else -> badAlgorithm()
                }
                if (dot.grounded()) {
                    sb.append('̸')
                }
//                when {
//                    side == -1 && capturedSide == -1 -> sb.append(RESET).append('·')
//                    capturedSide == -1 && side == 0 -> sb.append(FG_RED).append('●')//○
//                    capturedSide == -1 && side == 1 -> sb.append(FG_BLUE).append('●')
//
//                    capturedSide == 0 && side == -1 -> sb.append(FG_RED).append('·')
//                    capturedSide == 0 && side == 0 -> sb.append(FG_DARK_RED).append('○')
//                    capturedSide == 0 && side == 1 -> sb.append(FG_DARK_BLUE).append('●')
//
//                    capturedSide == 1 && side == -1 -> sb.append(FG_BLUE).append('·')
//                    capturedSide == 1 && side == 0 -> sb.append(FG_DARK_BLUE).append('●')
//                    capturedSide == 1 && side == 1 -> sb.append(FG_DARK_RED).append('○')
//                    else -> badAlgorithm()
//                }
                sb.append(RESET)
            }
            sb.append(y.toString().padStart(2))
            sb.append(' ')
            sb.appendLine()
        }
        sb.append(' ').append(' ')
        for (x in 0 until width) {
            sb.append(' ').append(x / 10)
        }
        sb.appendLine().append(' ').append(' ')
        for (x in 0 until width) {
            sb.append(' ').append(x % 10)
        }
        sb.appendLine()
        return sb.toString()
    }
    private fun dump() {
        print(colorDump())
        return
        println()
        println()
        for(y in 0 until height) {
            for(x in 0 until width) {
                print((field[index(x, y)].toInt() and 0xFF).toString(16).padStart(3))
            }
            println()
        }
        println()
        for(y in 0 until height) {
            for(x in 0 until width) {
                print(' ')
                print(field[index(x, y)].side() .let { if (it < 0) '.' else it})
                print(field[index(x, y)].capturedSide() .let { if (it < 0) '.' else it})
            }
            println()
        }
    }
    private fun checkCaptures(side: Int): Boolean {
        var hasCaptured = false
        /**
         * Находим все чужие точки не с краю
         */
        for(x in 1 until (width - 1)) {
            for(y in 1 until (height - 1)) {
                val dot = field[index(x, y)]
                val dotSide = dot.side()
                if (dotSide == side || dotSide == -1 || dot.capturedSide() != -1 || dot.flooded()) {
                    continue
                }
                val hasCap = !flood(x, y, side)
                val captured: Byte = if (hasCap) ((side + 1) shl 2).toByte() else 0
                if (hasCap) {
                    for (x1 in 0 until width) {
                        for (y1 in 0 until height) {
                            val index = index(x1, y1)
                            if (field[index].floodedNow()) {
                                field[index] = (field[index] and N_AND_CAP_SIDE_NEG) or captured
                            }
                        }
                    }
                } else {
                    for (x1 in 0 until width) {
                        for (y1 in 0 until height) {
                            val index = index(x1, y1)
                            field[index] = field[index] and N_NEG
                        }
                    }
                }
                if (hasCap) {
                    hasCaptured = true
                }
            }
        }
        for(x1 in 0 until width) {
            for (y1 in 0 until height) {
                val index = index(x1, y1)
                field[index] = field[index] and 0b100_1111
            }
        }
        return hasCaptured
    }

    /**
     * Возвращает true - если достигли стенки
     */
    private fun flood(x: Int, y: Int, side: Int): Boolean {
        if (x < 0 || x >= width  || y < 0 || y >= height) {
            return true
        }
        val index = index(x, y)
        val dot = field[index]
        if (dot.flooded() || dot.side() == side && dot.capturedSide() == -1) {
            return false
        }
        field[index] = field[index] or FN
        var wall = flood(x + 1, y, side)
        wall = flood(x - 1, y, side) || wall
        wall = flood(x, y + 1, side) || wall
        wall = flood(x, y - 1, side) || wall
        return wall
    }

    private fun countCaptured() {
        var s0 = 0
        var s1 = 0
        for(x in 1 until width - 1) {
            for(y in 1 until height - 1) {
                val dot = field[index(x, y)]
                val capturedSide = dot.capturedSide()
                val side = dot.side()
                if (side != -1 && capturedSide != -1 && capturedSide != dot.side()) {
                    when (capturedSide) {
                        0 -> s0++
                        1 -> s1++
                        else -> badAlgorithm()
                    }
                }
            }
        }
        score0 = s0
        score1 = s1
    }

    private fun floodGround(x: Int, y: Int, side: Int) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            val index = index(x, y)
            val dot = field[index]
            if (!dot.grounded() && (dot.side() == side || dot.capturedSide() == side)) {
                field[index] = field[index] or GND
                floodGround(x - 1, y, side)
                floodGround(x + 1, y, side)
                floodGround(x, y - 1, side)
                floodGround(x, y + 1, side)
            }
        }
    }

    private fun ground(side: Int) {
        for(x in 0 until width) {
            for(y in 0 until height step height - 1) {
                val dot = field[index(x, y)]
                if (!dot.grounded() && (dot.side() == side || dot.capturedSide() == side)) {
                    floodGround(x, y, side)
                }
            }
        }
        for(x in 0 until width step width - 1) {
            for(y in 0 until height) {
                val dot = field[index(x, y)]
                if (!dot.grounded() && (dot.side() == side || dot.capturedSide() == side)) {
                    floodGround(x, y, side)
                }
            }
        }
        for(x in 1 until width - 1) {
            for(y in 1 until height - 1) {
                val dot = field[index(x, y)]
                if (!dot.grounded() && (dot.side() == side || dot.capturedSide() == side)) {
                    for (d in 0..3) {
                        val dx = when (d) {
                            0 -> -1
                            1 -> 1
                            else -> 0
                        }
                        val dy = when (d) {
                            2 -> -1
                            3 -> 1
                            else -> 0
                        }
                        val dDot = field[index(x + dx, y + dy)]
                        if (dDot.grounded() && (dDot.side() == side || dDot.capturedSide() == side)) {
                            floodGround(x, y, side)
                            break
                        }
                    }
                }
            }
        }
    }

    data class GroundScore(val notGnd: Int, val free: Int)

    fun groundScore(side: Int): GroundScore {
        var notGnd = 0
        var free = 0
        for(x in 1 until width - 1) {
            for (y in 1 until height - 1) {
                val dot = field[index(x, y)]
                if (dot.grounded()) {
                    continue
                }
                val dotSide = dot.side()
                val dotCapSide = dot.capturedSide()

                if (dotSide == side && (dotCapSide == -1 || dotCapSide == side)) {
                    notGnd++
                } else if (dotSide != -1 && dotSide != side && dotCapSide == side) {
                    free ++
                }
            }
        }
        return GroundScore(notGnd, free)
    }
}
