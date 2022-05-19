package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConfirmForgotPasswordUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(newPassword: String, confirmCode: String): Flow<Unit> =
        repository.confirmForgotPassword(newPassword, confirmCode)
}