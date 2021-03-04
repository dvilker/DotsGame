package dotsgame.api.model

import dotsgame.server.Context
import zUtils.badUnknownParam
import zUtils.filterInPlace


object GOffers {
    val list = mutableListOf<GOffer>()

    fun toggle(side0: GUserId, side1: GUserId) {
        synchronized(GOffers) {
            val index = list.indexOfFirst { it.side0 == side0 && it.side1 == side1 }
            if (index >= 0) {
                // remove
                val offer = list.removeAt(index)
                Context.broadcastEvent(EvOfferDel(offer)) {
                    it.userId == offer.side0 || it.userId == offer.side1
                }
            } else {
                // make
                if (GBattles.exists(side0, side1, true, true)) {
                    throw badUnknownParam("Игра уже идет")
                }
                val offer = GOffer(side0, side1, GRules.default)
                list.add(offer)
                Context.broadcastEvent(EvOffersAdd(listOf(offer))) {
                    it.userId == offer.side0 || it.userId == offer.side1
                }
            }
        }
    }

    fun accept(side0: GUserId, side1: GUserId) {
        val offer: GOffer
        synchronized(GOffers) {
            val index = list.indexOfFirst { it.side0 == side0 && it.side1 == side1 }
            if (index < 0) {
                throw badUnknownParam("Предложение уже не актуально")
            }
            offer = list.removeAt(index)
            Context.broadcastEvent(EvOfferDel(offer)) {
                it.userId == offer.side0 || it.userId == offer.side1
            }
            GBattles.newFromOffer(offer)
        }
    }

    fun resetFor(userId: GUserId) {
        synchronized(GOffers) {
            list.filterInPlace { offer ->
                if (offer.side0 != userId && offer.side1 != userId) {
                    true
                } else {
                    val event = EvOfferDel(offer)
                    if (offer.side0 != userId) {
                        Context.broadcastEventFor(offer.side0, event)
                    }
                    if (offer.side1 != userId) {
                        Context.broadcastEventFor(offer.side1, event)
                    }
                    false
                }
            }
        }
    }

    fun sendFor(userId: GUserId) {
        synchronized(GOffers) {
            for (offer in list) {
                if (offer.side0 == userId || offer.side1 == userId) {
                    Context.broadcastEventFor(offer.side0, EvOffersAdd(listOf(offer)))
                }
            }
        }
    }
}