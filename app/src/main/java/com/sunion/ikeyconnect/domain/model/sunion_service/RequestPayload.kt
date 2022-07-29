package com.sunion.ikeyconnect.domain.model.sunion_service

import com.google.gson.annotations.SerializedName

data class DeviceProvisionCreateRequest(
    @SerializedName("ApplicationID") val applicationID: String,
    @SerializedName("Model") val model: String,
    @SerializedName("SerialNumber") val serialNumber: String,
    @SerializedName("DeviceName") val deviceName: String,
    @SerializedName("Timezone") val timezone: Timezone,
    @SerializedName("DataEncryptionKey") val dataEncryptionKey: String,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Timezone(
        @SerializedName("ShortName") val shortName: String,
        @SerializedName("Offset") val offset: Int,
    )
}

data class DeviceProvisionTicketGetRequest(
    @SerializedName("Ticket") val Ticket: String,
    @SerializedName("clientToken") val clientToken: String,
)

data class DeviceProvisionDeleteRequest(
    @SerializedName("DeviceIdentity") val deviceIdentity: String,
    @SerializedName("clientToken") val clientToken: String,
)

data class RequestPayload(
    @SerializedName("API") val api: String,
    @SerializedName("RequestBody") val requestBody: DeviceShadowUpdateLockRequest,
    @SerializedName("Authorization") val authorization: String,
)

data class DeviceShadowUpdateLockRequest(
    @SerializedName("DeviceIdentity") val deviceIdentity: String,
    @SerializedName("Desired") val desired: Desired,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Desired(
        @SerializedName("Deadbolt") val deadbolt: String,
//        @SerializedName("Searchable") val searchable: Int,
    )
}

data class DeviceShadowUpdateRunCheckRequest(
    @SerializedName("DeviceIdentity") val deviceIdentity: String,
    @SerializedName("Desired") val desired: Desired,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Desired(
        @SerializedName("Direction") val direction: String,
//        @SerializedName("Searchable") val searchable: Int,
    )
}

data class EventGetRequest(
    @SerializedName("DeviceIdentity") val DeviceIdentity: String,
    @SerializedName("Filter") val filter: Filter,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Filter(
        @SerializedName("TimePoint") val TimePoint: Int,
        @SerializedName("Maximum") val Maximum: Int,
    )
}

data class UserSyncGetRequest(
    @SerializedName("DeviceIdentity") val DeviceIdentity: String,
    @SerializedName("Filter") val filter: Filter,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Filter(
        @SerializedName("TimePoint") val TimePoint: Int,
        @SerializedName("Maximum") val Maximum: Int,
    )
}