package com.sunion.ikeyconnect.domain.exception

sealed class LockStatusException : Throwable() {
    class LockStatusNotRespondingException : LockStatusException()
    class AdminCodeNotSetException : LockStatusException()
    class LockOrientationException : LockStatusException()
}
