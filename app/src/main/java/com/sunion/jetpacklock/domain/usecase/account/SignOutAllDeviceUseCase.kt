package com.sunion.jetpacklock.domain.usecase.account

import com.sunion.jetpacklock.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignOutAllDeviceUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<Unit> = authRepository.signOutAllDevice()
}