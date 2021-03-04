package dotsgame.api

import dotsgame.api.model.GUser
import dotsgame.api.model.GUserId

/**
 * Пользователь только подключился
 */
data class EvInit(
    val user: GUser?,
    val online: Collection<GUserId>,
): Ev("init")