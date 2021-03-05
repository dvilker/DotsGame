package dotsgame.entities

import dotsgame.enums.*
import zDb.entities.Entity
import zDb.entities.annotations.EntityInfo
import java.time.LocalDateTime

/**
 * Пользователи
 */
@EntityInfo([DEFAULT_SCHEMA])
interface Battle: Entity {

    companion object: zDb.entities.EntityDescriptor<Battle>() {

        override fun Indexes.indexes() {
        }

        override fun Defaults.defaults() {
            default(Battle::ruleSize, RuleSize.default)
            default(Battle::ruleStart, RuleStart.default)
            default(Battle::ruleTimer, RuleTimer.default)
        }

        override fun Automations.automations() {
            constraint3(
                "Поля должны содержать 2 элемента",
                " cardinality(sides) = 2 AND cardinality(captured_points) = 2 AND cardinality(score_change) = 2 "
            )
        }
    }

    var room: Room

    var startTime: LocalDateTime

    var overTime: LocalDateTime

    var over: BattleOver

    var moves: String

    var winSide: Int?

    var sides: List<User>

    var capturedPoints: List<Int>

    var levels: List<UserLevel>

    var scoreBefore: List<Int>

    var scoreChange: List<Int>

    val ruleSize: RuleSize
    val ruleStart: RuleStart
    val ruleTimer: RuleTimer

}
