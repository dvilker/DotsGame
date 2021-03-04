package dotsgame.entities

import zDb.entities.Entity
import zDb.finder.FullTextColumns
import zDb.finder.fullTextSearch
import zUtils.nullIfBlank
import kotlin.reflect.KProperty1

const val DEFAULT_SCHEMA: String = "dotsgame"

// for titleTagsSearch
const val TITLE_TAGS_SEARCH_INDEX = "CREATE INDEX :i ON :t USING gin ((  to_tsvector('simple', coalesce(upper(title), '')) || to_tsvector('simple', coalesce(upper(tags), ''))  ))"

fun <E: Entity>titleTagsSearch(search: String?, titleProp: KProperty1<E, String?>, tagsProp: KProperty1<E, String?>)
        = search.nullIfBlank()?.let { FullTextColumns(titleProp, tagsProp, coalesceToEmpty = true, upper = true) fullTextSearch it.toUpperCase() }