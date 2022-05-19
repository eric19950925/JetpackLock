package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConfirmSignUpUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(email: String, confirmCode: String): Flow<Boolean> =
        authRepository.confirmSignUp(email, confirmCode)
}