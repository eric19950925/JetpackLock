package com.sunion.ikeyconnect.domain.model

sealed class LockOrientation : Throwable() {
    object Right : LockOrientation()
    object Left : LockOrientation()
    object NotDetermined : LockOrientation()
}
