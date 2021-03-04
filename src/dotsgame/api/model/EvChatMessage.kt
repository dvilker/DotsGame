package dotsgame.api.model

import dotsgame.api.Ev

data class EvChatMessage(val roomId: GRoomId, val msg: GChatMessage): Ev("msg")