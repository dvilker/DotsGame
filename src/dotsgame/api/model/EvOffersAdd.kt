package dotsgame.api.model

import dotsgame.api.Ev

data class EvOffersAdd(val offers: List<GOffer>): Ev("offer+")