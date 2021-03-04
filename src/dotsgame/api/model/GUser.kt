package dotsgame.api.model

import dotsgame.entities.User
import dotsgame.enums.UserLevel
import zUtils.myjson.JsonTransient

open class GUser(
    @JsonTransient
    val user: User,
) {
    val id: GUserId get() = user.id
    val title: String get() = user.title
    val pic: String? get() = user.pic
    val level: UserLevel get() = user.level
    val score: Int get() = user.score

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? GUser)?.id == id
    }
}
