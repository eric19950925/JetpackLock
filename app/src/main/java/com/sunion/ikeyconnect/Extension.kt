package com.sunion.ikeyconnect

import org.json.JSONObject

inline fun unless(condition: Boolean, crossinline block: () -> Unit) {
    if (condition) {
        block.invoke()
    }
}

fun JSONObject.optNullableString(name: String, fallback: String? = null) : String? {
    return if (this.has(name) && !this.isNull(name)) {
        this.getString(name)
    } else {
        fallback
    }
}
