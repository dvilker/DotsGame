package dotsgame.api

import dotsgame.api.model.GBattleId
import dotsgame.api.model.GBattles
import dotsgame.server.threadContext
import zApi.api.ApiMethod


@ApiMethod
fun battleMove(battleId: GBattleId, x: Int, y: Int) {
    GBattles.move(threadContext.game, battleId, x, y)
}

@ApiMethod
fun battleSurrender(battleId: GBattleId) {
    GBattles.surrender(threadContext.game, battleId)
}

@ApiMethod
fun battleDraw(battleId: GBattleId, ask: Boolean) {
    GBattles.draw(threadContext.game, battleId, ask)
}