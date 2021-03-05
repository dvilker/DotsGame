package dotsgame.api.model

import dotsgame.entities.User

class GUserMe(
    user: User
): GUser(user) {
    val name get() = user.name
    val ruleSize get() = user.ruleSize
    val ruleStart get() = user.ruleStart
    val ruleTimer get() = user.ruleTimer

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? GUser)?.id == id
    }
}
