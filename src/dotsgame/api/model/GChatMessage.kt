package dotsgame.api.model

import java.time.LocalDateTime

class GChatMessage(
    val time: LocalDateTime,
    val move: Int?,
    val from: GUserId?,
    val to: Set<GUserId>?,
    val text: String,
    val me: Boolean = false,
    val sys: Boolean = false,
)