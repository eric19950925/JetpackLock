package com.sunion.jetpacklock.domain.usecase.account

import com.sunion.jetpacklock.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ResendSignUpConfirmCodeUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(email: String): Flow<Unit> = repository.resendSignUpConfirmCode(email)
}