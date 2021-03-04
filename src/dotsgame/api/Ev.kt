package dotsgame.api

import zUtils.myjson.JsonTransient
import zUtils.myjson.formatJson
import zUtils.myjson.rootJsonConverter
import java.io.StringWriter

abstract class Ev (
    @JsonTransient
    val _name: String
) {
    @JsonTransient
    val eventJson: String
    get() {
        @Suppress("UNCHECKED_CAST")
        val eventOb = rootJsonConverter.valueToParsed(this, null) as MutableMap<String, Any?>
        eventOb["_"] = _name
        return StringWriter().use { out->
            formatJson(eventOb, out)
            out.toString()
        }
    }
}