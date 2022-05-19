package com.sunion.ikeyconnect.domain.usecase.account

import com.google.gson.Gson
import com.sunion.ikeyconnect.MainActivity
import com.sunion.ikeyconnect.api.AccountAPI
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ShareInvitationUseCase @Inject constructor(private val accountAPI: AccountAPI) {
    private val gson = Gson()
    private val MEDIA_TYPE_JSON = MainActivity.CONTENT_TYPE_JSON.toMediaType()
    val mSetNotifyPayload = InvitationPayload(
        Share = arrayOf(
            ShareContent(
            Resource = ResourceContent(
                DeviceIdentity = "c7310620-9fb3-42f5-b75f-2e8b6393ddcd",//postman - thing name
                ResourceDirectory = ""
            ),
            Attributes = AttributesContent(UserRole = "Manager")
            )
        ),
        clientToken = "AAA"
    )
    val postBody = gson.toJson(mSetNotifyPayload).toString()
    operator fun invoke(idToken: String) = flow {
        accountAPI.createShareInvitation(idToken = "Bearer $idToken", postBody = postBody.toRequestBody(MEDIA_TYPE_JSON))
        emit(Unit)
    }
}
data class InvitationPayload(
    val Share: Array<ShareContent>,
    val clientToken: String
)

data class ShareContent(
    val Resource: ResourceContent,
    val Attributes: AttributesContent
)

data class ResourceContent(
    val DeviceIdentity: String,
    val ResourceDirectory: String
)
data class AttributesContent(
    val UserRole: String,
)