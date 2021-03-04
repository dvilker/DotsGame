package dotsgame.api

import dotsgame.api.model.GUser

/**
 * Пользователь подключился
 */
data class EvUserAdd(val user: GUser): Ev("user+")