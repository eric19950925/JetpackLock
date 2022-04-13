package com.sunion.jetpacklock

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<Unit> = authRepository.signOut()
}