package dotsgame.api

import dotsgame.api.model.GBattles
import dotsgame.api.model.GOffers
import dotsgame.api.model.GUser
import dotsgame.server.Context
import dotsgame.server.threadContext

fun connected() {
    val context = threadContext
    val user = context.user
    val wasRemoved = Context.connections.removeIf {
        if (it !== context && it.user == user) {
            it.dropConnection()
            true
        } else {
            false
        }
    }
    context.sendEvent(EvInit(
        user?.let { GUser(it) },
        Context.connections.mapNotNull { it.userId }
    ))
    user?.let {
        GOffers.sendFor(it.id)
        GBattles.sendHeadersFor(context.game)
    }
    if (wasRemoved) {
        context.exUserOrNull?.let {
            Context.broadcastEvent(EvUser(it)) { it != context }
        }
    } else {
        context.exUserOrNull?.let {
            Context.broadcastEvent(EvUserAdd(it)) { it != context }
        }
    }
}

fun disconnected() {
    threadContext.exUserOrNull?.let {
        GOffers.resetFor(it.id)
        Context.broadcastEvent(EvUserDel(it))
    }
}