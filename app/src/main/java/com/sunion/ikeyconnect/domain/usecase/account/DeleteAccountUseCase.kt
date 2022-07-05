package com.sunion.ikeyconnect.domain.usecase.account

import com.google.gson.Gson
import com.sunion.ikeyconnect.MainActivity
import com.sunion.ikeyconnect.api.AccountAPI
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val idTokenUseCase: GetIdTokenUseCase,
    private val accountAPI: AccountAPI,
    private val signOutAllDeviceUseCase: SignOutAllDeviceUseCase
    ) {
    private val gson = Gson()
    private val MEDIA_TYPE_JSON = MainActivity.CONTENT_TYPE_JSON.toMediaType()

    val mDeleteAccountPayload = DeleteAccountPayload(
        clientToken = "AAA"
    )
    val postBody = gson.toJson(mDeleteAccountPayload).toString()
    operator fun invoke(password: String) = flow {
        changePasswordUseCase(password, password).single()
        accountAPI.deleteUser(idToken = "Bearer ${idTokenUseCase().single()}", postBody = postBody.toRequestBody(MEDIA_TYPE_JSON))
        signOutAllDeviceUseCase().single()
        emit(Unit)
    }
}

data class DeleteAccountPayload(
    val clientToken: String
)