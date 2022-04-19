package com.sunion.jetpacklock.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signIn(username: String, password: String): Flow<String>
    fun signOut(): Flow<Unit>
    fun getIdToken(): Flow<String>
}