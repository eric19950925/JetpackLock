package com.sunion.jetpacklock.domain.usecase

import com.sunion.jetpacklock.api.AccountAPI
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(private val accountAPI: AccountAPI) {
    operator fun invoke(idToken: String) = flow {
        accountAPI.deleteUser(idToken = "Bearer $idToken")
        emit(Unit)
    }
}