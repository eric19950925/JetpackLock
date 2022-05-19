package com.sunion.ikeyconnect.domain.exception

open class IKeyException(message: String) : Exception(message)

open class AuthException(message: String) : IKeyException(message)

interface UsernameException

class UsernameExistsException : AuthException("Username exists."), UsernameException

class UserNotFoundException : AuthException("User does not exist."), UsernameException