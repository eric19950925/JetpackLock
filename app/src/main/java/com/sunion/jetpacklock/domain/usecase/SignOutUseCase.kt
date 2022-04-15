package com.sunion.jetpacklock.domain.usecase

import com.sunion.jetpacklock.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<Unit> = authRepository.signOut()
}