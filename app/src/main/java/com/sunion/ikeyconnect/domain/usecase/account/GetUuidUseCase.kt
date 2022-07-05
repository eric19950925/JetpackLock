package com.sunion.ikeyconnect.domain.usecase.account

import com.sunion.ikeyconnect.domain.Interface.AuthRepository
import javax.inject.Inject

class GetUuidUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke() = authRepository.getUuid()
}