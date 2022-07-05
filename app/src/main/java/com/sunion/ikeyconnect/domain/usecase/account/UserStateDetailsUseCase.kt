package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.Interface.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class UserStateDetailsUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<String?> = authRepository.getName().take(1)
}