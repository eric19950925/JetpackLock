package com.sunion.jetpacklock

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signIn(username: String, password: String): Flow<String>
    fun signOut(): Flow<Unit>
}