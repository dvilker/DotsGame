package dotsgame.api.model

import dotsgame.api.Ev

data class EvRoom(val room: GRoom, val messages: List<GChatMessage>): Ev("room")