package dotsgame.api

import dotsgame.api.model.GOffers
import dotsgame.server.Context
import dotsgame.server.threadContext
import zApi.api.ApiMethod
import zUtils.badParam


@ApiMethod
fun toggleOffer(withUserId: Long) {
    synchronized(Context.connections) {
        GOffers.toggle(
            threadContext.userIdNN,
            Context.connections.firstOrNull { it.userId == withUserId }?.userIdNN
                ?: badParam("withUserId", "Пользователь не в игре")
        )
    }
}

@ApiMethod
fun acceptOffer(withUserId: Long) {
    synchronized(Context.connections) {
        GOffers.accept(
            Context.connections.firstOrNull { it.userId == withUserId }?.userIdNN
                ?: badParam("withUserId", "Пользователь не в игре"),
            threadContext.userIdNN
        )
    }
}
