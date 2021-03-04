package dotsgame.api.model

import dotsgame.enums.BattleOver
import zUtils.DiList

class GOver(
    val over: BattleOver,
    val winSide: Int?,
    val scoreBefore: DiList<Int>,
    val scoreChange: DiList<Int>,
)