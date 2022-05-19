package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(oldPassword: String, newPassword: String): Flow<Unit> =
        authRepository.changePassword(oldPassword, newPassword)
}