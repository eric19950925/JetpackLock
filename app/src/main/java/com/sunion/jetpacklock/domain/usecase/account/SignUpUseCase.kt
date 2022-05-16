package com.sunion.jetpacklock.domain.usecase.account

import com.sunion.jetpacklock.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignUpUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(email: String, password: String): Flow<Boolean> =
        authRepository.signUp(email, password)
}