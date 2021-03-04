package dotsgame.entities

import zDb.entities.Entity
import zDb.entities.annotations.EntityInfo

/**
 * Пользователи
 */
@EntityInfo([DEFAULT_SCHEMA])
interface Room: Entity {

    companion object: zDb.entities.EntityDescriptor<Room>() {

        override fun Indexes.indexes() {
        }

        override fun Defaults.defaults() {
        }
    }

    var battle: Battle?
}
