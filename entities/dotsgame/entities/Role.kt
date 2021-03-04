package dotsgame.entities

import zDb.entities.annotations.Defaulted
import zDb.entities.annotations.EntityInfo

/**
 * Пользователи
 */
@EntityInfo([DEFAULT_SCHEMA])
interface Role : ITitled {

    companion object: zDb.entities.EntityDescriptor<Role>() {

        override fun Indexes.indexes() {
        }

        override fun Defaults.defaults() {
            default(Role::isSysAdmin, false)
        }
    }

    @Defaulted
    var isSysAdmin: Boolean

}
