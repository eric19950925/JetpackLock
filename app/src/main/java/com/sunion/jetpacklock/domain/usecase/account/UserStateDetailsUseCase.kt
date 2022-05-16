package com.sunion.jetpacklock.domain.usecase.account

import com.sunion.jetpacklock.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class UserStateDetailsUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<String?> = authRepository.getName().take(1)
}