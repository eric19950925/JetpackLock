package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(email: String): Flow<Unit> = repository.requestForgotPassword(email)
}