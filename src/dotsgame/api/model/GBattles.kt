package dotsgame.api.model

import dotsgame.entities.Battle
import dotsgame.entities.Room
import dotsgame.enums.BattleOver
import dotsgame.server.Context
import zDb.getDb
import zDb.releaseAllDbs
import zUtils.badAlgorithm
import zUtils.badUnknownParam
import zUtils.filterInPlace
import zUtils.log.logWarning
import kotlin.concurrent.thread

object GBattles {
    val list = mutableListOf<GBattle>()

    private val timerThread = thread(start = true, isDaemon = true, name = "GBattlesTimer") {
        val thread = Thread.currentThread()
        while (!thread.isInterrupted) {
            try {
                Thread.sleep(1000 - System.currentTimeMillis() % 1000)
                tick()
            } catch (t: InterruptedException) {
                break
            } catch (t: Throwable) {
                logWarning("tick(): {0}", t)
            }
        }
    }

    fun stop() {
        timerThread.interrupt()
        timerThread.join()
        // And save games to disk
    }

    fun exists(side0: GUserId, side1: GUserId, biDirect: Boolean, passOver: Boolean): Boolean {
        synchronized(GBattles) {
            return if (biDirect) {
                list.any {
                    (
                            it.side0.userId == side0 && it.side1.userId == side1
                                    || it.side0.userId == side1 && it.side1.userId == side0
                            )
                            && (!passOver || it.over == null)
                }
            } else {
                list.any { it.side0.userId == side0 && it.side1.userId == side1 && (!passOver || it.over == null) }
            }
        }
    }

    fun byIdOrNull(id: GBattleId): GBattle? {
        synchronized(GBattles) {
            return list.firstOrNull { it.id == id }
        }
    }

    fun byId(id: GBattleId): GBattle = byIdOrNull(id) ?: badUnknownParam("Игра {0} не найдена", id)

    fun tick() {
        try {
            synchronized(GBattles) {
                val now = System.currentTimeMillis()
                list.filterInPlace { battle ->
                    if (battle.over == null) {
                        if (now > battle.moveStartTime + (battle.side().totalTime + battle.rules.timer.moveTime) * 1000) {
                            battle.over(
                                BattleOver.TIMEOUT,
                                if (battle.moves.length > 10) {
                                    1 - battle.moveSide
                                } else {
                                    null
                                }
                            )
                        }
                        true
                    } else if (now - battle.moveStartTime > 60_000) {
                        Context.broadcastEvent(EvBattleDel(battle.id))
                        false
                    } else {
                        true
                    }
                }
            }
        } finally {
            releaseAllDbs()
        }
    }

    fun newFromOffer(offer: GOffer) {
        synchronized(GBattles) {
            if (exists(offer.side0, offer.side1, true, true)) {
                throw badUnknownParam("Игра уже идет")
            }
            val battle = GBattle(
                GBattle.Side(offer.side0, offer.rules),
                GBattle.Side(offer.side1, offer.rules),
                offer.rules
            )
            list.add(battle)
            Context.broadcastEvent(EvBattlesAdd(listOf(battle.header)))
            GRooms.selectForBattle(null, battle)
        }
    }

    fun sendBattleFor(connection: GConnection, battleId: GBattleId) {
        synchronized(GBattles) {
            val battle = byId(battleId)
            connection.context.sendEvent(EvBattle(battle))
            connection.context.sendEvent(
                EvBattleMove(
                    battle.id,
                    0,
                    battle.moves,
                    battle.moveSide,
                    battle.side0.points,
                    battle.side1.points,
                    battle.side0.totalTime,
                    battle.side1.totalTime,
                    battle.moveStartTime
                )
            )
        }
    }

    fun sendHeadersFor(connection: GConnection) {
        synchronized(GBattles) {
            connection.context.sendEvent(EvBattlesAdd(list.map { it.header }))
        }
    }

    fun move(connection: GConnection, battleId: GBattleId, x: Int, y: Int) {
        synchronized(GBattles) {
            val battle = byId(battleId)
            if (battle.over != null) {
                badUnknownParam("Партия окончена")
            }
            val side = when (battle.moveSide) {
                0 -> battle.side0
                1 -> battle.side1
                else -> badAlgorithm()
            }
            if (side.userId != connection.userId) {
                badUnknownParam("Не ваш ход")
            }
            battle.move(x, y)
        }
    }

    fun surrender(connection: GConnection, battleId: GBattleId) {
        synchronized(GBattles) {
            val battle = byId(battleId)
            val side = when (connection.userId) {
                battle.side0.userId -> 0
                battle.side1.userId -> 1
                else -> badUnknownParam("Не ваша игра")
            }
            battle.over(BattleOver.SURRENDER, 1 - side)
        }
    }

    fun draw(connection: GConnection, battleId: GBattleId, ask: Boolean) {
        synchronized(GBattles) {
            val battle = byId(battleId)
            val side = when (connection.userId) {
                battle.side0.userId -> 0
                battle.side1.userId -> 1
                else -> badUnknownParam("Не ваша игра")
            }
            val sideX = when (connection.userId) {
                battle.side0.userId -> battle.side0
                battle.side1.userId -> battle.side1
                else -> badUnknownParam("Не ваша игра")
            }
            if (battle.over != null) {
                badUnknownParam("Партия уже окончена")
            }
            if (ask) {
                if (battle.askDraw != null && battle.askDraw != side) {
                    // Была запрошена ничья и на нее согласились
                    battle.askDraw = null
                    battle.over(BattleOver.DRAW, null)
                } else if (battle.askDraw == null) {
                    if (sideX.lastAskDrawMove == battle.moves.length) {
                        badUnknownParam("Слишком часто")
                    } else {
                        sideX.lastAskDrawMove = battle.moves.length
                        battle.askDraw = side
                        Context.broadcastEventToBattle(battleId, EvBattleAskDraw(battleId, battle.askDraw))
                        battle.room.addSysMessage(connection.userId, "предлагает ничью.")
                    }
                }
            } else {
                if (battle.askDraw != null) {
                    battle.askDraw = null
                    Context.broadcastEventToBattle(battleId, EvBattleAskDraw(battleId, battle.askDraw))
                    battle.room.addSysMessage(connection.userId, "отказывается от ничьи.")
                }
            }
        }
    }

    fun newBattleId(): GBattleId {
        val e = Battle(null)
        getDb().acquireId(e)
        return e.id
    }
}