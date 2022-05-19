package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.repository.AuthRepository
import javax.inject.Inject

class GetIdTokenUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke() = authRepository.getIdToken()
}