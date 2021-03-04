package dotsgame.api.model

import dotsgame.server.Context
import zUtils.myjson.JsonTransient
import java.time.LocalDateTime

class GRoom(
    @JsonTransient
    val battleId: GBattleId?,
    val id: GRoomId = GRooms.newRoomId(),
) {
    @JsonTransient
    val messages = mutableListOf<GChatMessage>()

    fun addMessage(from: GUserId, to: Set<GUserId>?, text: String) {
        synchronized(this) {
            val msg = GChatMessage(
                LocalDateTime.now(),
                battleId?.let {
                    GBattles.byIdOrNull(it)?.field?.moveCount
                },
                from,
                to,
                text
            )
            messages.add(msg)
            Context.broadcastEventToRoom(id, EvChatMessage(id, msg))
        }
    }

    fun addSysMessage(meUser: GUserId?, text: String) {
        synchronized(this) {
            val msg = GChatMessage(
                LocalDateTime.now(),
                battleId?.let {
                    GBattles.byIdOrNull(it)?.field?.moveCount
                },
                meUser,
                null,
                text,
                me = meUser != null,
                sys = true
            )
            messages.add(msg)
            Context.broadcastEventToRoom(id, EvChatMessage(id, msg))
        }
    }
}