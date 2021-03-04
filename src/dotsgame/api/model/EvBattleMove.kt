package dotsgame.api.model

import dotsgame.api.Ev

data class EvBattleMove(
    val battleId: GBattleId,
    val offset: Int,
    val moves: CharSequence,
    val moveSide: Int,
    val p0: Int,
    val p1: Int,
    val t0: Int,
    val t1: Int,
    val moveStartTime: Long,
    val now: Long = System.currentTimeMillis(),
): Ev("battleMove")