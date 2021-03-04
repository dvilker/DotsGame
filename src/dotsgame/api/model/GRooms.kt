package dotsgame.api.model

import dotsgame.entities.Room
import dotsgame.server.Context
import zDb.getDb
import zUtils.badUnknownParam

object GRooms {
    val list = mutableListOf<GRoom>()

    fun forBattle(battleId: GBattleId): GRoom {
        synchronized(GRooms) {
            return list.firstOrNull { it.battleId == battleId }
                ?: run {
                    val room = GRoom(
                        battleId
                    )
                    list.add(room)
                    room
                }
        }
    }

    fun select(connection: GConnection, roomId: GRoomId) {
        synchronized(GRooms) {
            val room = list.firstOrNull { it.id == roomId } ?: badUnknownParam("Комната {0} не найдена", roomId)
            connection.room = room
            synchronized(room) {
                connection.context.sendEvent(EvRoom(room, room.messages.toList()))
            }
            if (room.battleId != null) {
                GBattles.sendBattleFor(connection, room.battleId)
            }
        }
    }
    fun selectForBattle(connection: GConnection?, battle: GBattle) {
        synchronized(GRooms) {
            val room = battle.room
            if (connection != null) {
                select(connection, room.id)
            } else {
                synchronized(Context.connections) {
                    for (context in Context.connections) {
                        if (context.userId == battle.side0.userId || context.userId == battle.side1.userId) {
                            select(context.game, room.id)
                        }
                    }
                }
            }
        }
    }

    fun newRoomId(): GRoomId {
        val e = Room(null)
        getDb().acquireId(e)
        return e.id
    }
}