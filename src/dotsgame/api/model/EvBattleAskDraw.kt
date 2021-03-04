package dotsgame.api.model

import dotsgame.api.Ev

data class EvBattleAskDraw(val battleId: GBattleId, val askDraw: Int?): Ev("battleDraw")