package dotsgame.api

import dotsgame.api.model.GUser

/**
 * Пользователь отключился
 */
data class EvUserDel(val user: GUser): Ev("user-")