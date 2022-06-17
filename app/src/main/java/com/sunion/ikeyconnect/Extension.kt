package com.sunion.ikeyconnect

inline fun unless(condition: Boolean, crossinline block: () -> Unit) {
    if (condition) {
        block.invoke()
    }
}
