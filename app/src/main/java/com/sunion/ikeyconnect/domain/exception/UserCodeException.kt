package com.sunion.ikeyconnect.domain.exception

sealed class UserCodeException : Throwable() {
    class AddAdminCodeException : UserCodeException()
    class UserCodeExceededException : UserCodeException()
    class AddUserCodeFailException : UserCodeException()
    class GetUserCodeFailException : UserCodeException()
}
