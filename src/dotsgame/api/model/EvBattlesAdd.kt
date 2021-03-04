package dotsgame.api.model

import dotsgame.api.Ev

data class EvBattlesAdd(val battles: List<GBattleHeader>): Ev("battle+")