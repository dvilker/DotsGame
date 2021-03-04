package dotsgame.api.model

import zUtils.myjson.JsonTransient

class GBattleHeader (@JsonTransient val battle: GBattle) {
    val id: GBattleId = battle.id
    val roomId: GRoomId = battle.roomId
    val side0: GUserId = battle.side0.userId
    val side1: GUserId = battle.side1.userId
    val rules: GRules = battle.rules
    val isOver: Boolean get() = battle.over != null
}