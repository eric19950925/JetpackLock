package com.sunion.jetpacklock.domain.usecase.account

import com.sunion.jetpacklock.MainActivity
import com.sunion.jetpacklock.api.AccountAPI
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class AttachPolicyUseCase @Inject constructor(private val accountAPI: AccountAPI) {
    private val MEDIA_TYPE_JSON = MainActivity.CONTENT_TYPE_JSON.toMediaType()
    private val postBody = "{\"clientToken\":\"AAA\"}"

    operator fun invoke(idToken: String) = flow {
        accountAPI.attachPolicy(idToken = "Bearer $idToken", postBody = postBody.toRequestBody(MEDIA_TYPE_JSON))
        emit(Unit)
    }
}