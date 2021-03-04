package dotsgame.api

import dotsgame.api.model.GRoomId
import dotsgame.api.model.GRooms
import dotsgame.api.model.GUserId
import dotsgame.server.Context
import dotsgame.server.threadContext
import zApi.api.ApiMethod
import zUtils.badUnknown
import zUtils.badUnknownParam
import zUtils.normalizeLine

@ApiMethod(needAuth = false)
fun selectRoom(roomId: GRoomId) {
    GRooms.select(threadContext.game, roomId)
}

@ApiMethod
fun addMessage(text: String, to: Set<GUserId>? = null) {
    val room = threadContext.game.room ?: badUnknownParam("Вы не в комнате")
    if (to != null && to.isEmpty()) {
        badUnknown()
    }
    if (to != null) {
        synchronized(Context.connections) {
            to@for(userId in to) {
                for (connection in Context.connections) {
                    if (connection.userId == userId && connection.game.room == room) {
                        continue@to
                    }
                    badUnknownParam("Адресата нет в комнате")
                }
            }
        }
    }
    val textNorm = text.normalizeLine()
    if (textNorm.length > 140) {
        badUnknownParam("Слишком длинное сообщение")
    }
    room.addMessage(threadContext.userIdNN, to, textNorm)
}