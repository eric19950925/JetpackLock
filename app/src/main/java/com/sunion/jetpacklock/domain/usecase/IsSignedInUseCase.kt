package com.sunion.jetpacklock.domain.usecase

import com.sunion.jetpacklock.domain.repository.AuthRepository
import javax.inject.Inject

class IsSignedInUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): Boolean = authRepository.isSignedIn()
}