package com.sunion.jetpacklock.domain.usecase

import com.sunion.jetpacklock.api.AccountAPI
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetTimeUseCase @Inject constructor(private val accountAPI: AccountAPI) {

    operator fun invoke(idToken: String) = flow {
        val response = accountAPI.getTime(
            idToken = "Bearer $idToken",
            timezone = "Asia/Taipei",
            clienttoken = "AAA"
            )
        emit(response)
    }
}

data class TimeResponse (
    val UnixTimestamp: Long,
    val Offset: Long,
    val message: String,
    val clientToken: String
)