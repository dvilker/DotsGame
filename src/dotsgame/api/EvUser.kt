package dotsgame.api

import dotsgame.api.model.GUser

/**
 * Пользователь обновил анкету
 */
data class EvUser(val user: GUser): Ev("user")