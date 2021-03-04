package dotsgame.api

import dotsgame.api.model.GUser
import dotsgame.api.model.GUserId
import dotsgame.api.model.GUserMe
import dotsgame.entities.User
import dotsgame.server.threadContext
import zApi.api.ApiMethod
import zDb.find
import zDb.finder.equalsAny
import zDb.getDb
import zUtils.badUnknown

private val getUsersLock = Any()

@ApiMethod(needAuth = false)
fun getUsers(ids: List<GUserId>): List<GUser> {
    synchronized(getUsersLock) {
        if (ids.size > 32) {
            badUnknown()
        }
        return getDb().find(
            User::id equalsAny ids,
            User::title,
            User::name,
            User::pic,
            User::level,
            User::score,
        ).getList().map { GUser(it) }
    }
}

@ApiMethod
fun getMe(): GUserMe {
    return GUserMe(threadContext.userNN)
}