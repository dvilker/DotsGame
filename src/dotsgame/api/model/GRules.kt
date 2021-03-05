package dotsgame.api.model

import dotsgame.entities.User
import dotsgame.enums.RuleSize
import dotsgame.enums.RuleStart
import dotsgame.enums.RuleTimer

data class GRules (
    val start: RuleStart,
    val timer: RuleTimer,
    val size: RuleSize,
) {
    constructor(user: User): this(user.ruleStart, user.ruleTimer, user.ruleSize)
}