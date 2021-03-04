package dotsgame.api.model

import dotsgame.api.Ev

data class EvBattleOver(val battleId: GBattleId, val over: GOver): Ev("battleOver")