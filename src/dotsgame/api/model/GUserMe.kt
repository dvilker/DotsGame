package dotsgame.api.model

import dotsgame.entities.User

class GUserMe(
    user:User
): GUser(user) {
    val name: String get() = user.name
    val rules: String  get() = user.rules

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? GUser)?.id == id
    }
}
