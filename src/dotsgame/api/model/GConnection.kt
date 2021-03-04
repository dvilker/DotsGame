package dotsgame.api.model

import dotsgame.server.Context

class GConnection(val context: Context) {
    var room: GRoom? = null

    val userIdOrNull: GUserId? get() = context.userId
    val userId: GUserId get() = context.userIdNN
    val userOrNull: GUser? get() = context.exUserOrNull
    val user: GUser get() = context.exUser
}