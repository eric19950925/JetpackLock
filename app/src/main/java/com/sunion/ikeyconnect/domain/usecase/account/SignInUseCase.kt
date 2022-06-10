package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.Interface.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(email: String, password: String): Flow<String> = authRepository.signIn(email, password)
}