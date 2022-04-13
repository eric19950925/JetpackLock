package com.sunion.jetpacklock

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(email: String, password: String): Flow<String> = authRepository.signIn(email, password)
}