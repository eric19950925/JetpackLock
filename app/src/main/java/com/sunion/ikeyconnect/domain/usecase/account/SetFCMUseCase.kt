package com.sunion.ikeyconnect.domain.usecase.account

import com.google.gson.Gson
import com.sunion.ikeyconnect.MainActivity.Companion.CONTENT_TYPE_JSON
import com.sunion.ikeyconnect.api.AccountAPI
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class SetFCMUseCase @Inject constructor(
    private val accountAPI: AccountAPI,
    private val gson: Gson,
) {
    private val MEDIA_TYPE_JSON = CONTENT_TYPE_JSON.toMediaType()
    private val mSetNotifyPayload = SetNotifyPayload(
        Type = 0,
        Token = "FCMtoken",
        ApplicationID = "Sunion_20200617",
        Enable = true,
        LanguageLocalisation = "en-US",
        clientToken = "AAA"
    )
    val postBody = gson.toJson(mSetNotifyPayload).toString()

    operator fun invoke(idToken: String) = flow {
        accountAPI.setFCM(idToken = "Bearer $idToken", postBody = postBody.toRequestBody(MEDIA_TYPE_JSON))
        emit(Unit)
    }
}
data class SetNotifyPayload(
    val Type: Int,
    val Token: String,
    val ApplicationID: String,
    val Enable: Boolean,
    val LanguageLocalisation: String,
    val clientToken: String
)