package com.sunion.ikeyconnect.domain.model.sunion_service

import com.google.gson.annotations.SerializedName

open class DeviceRegistryUpdateRequest(
    @SerializedName("DeviceIdentity") open val deviceIdentity: String,
    @SerializedName("Payload") open val payload: DeviceRegistryRequestPayload,
    @SerializedName("clientToken") open val clientToken: String,
)

open class DeviceRegistryRequestPayload


data class DeviceRegistryUpdateNameRequest(
    @Transient @SerializedName("DeviceIdentity") override val deviceIdentity: String,
    @Transient @SerializedName("Payload") override val payload: Payload,
    @Transient @SerializedName("clientToken") override val clientToken: String,
) : DeviceRegistryUpdateRequest(deviceIdentity, payload, clientToken) {
    data class Payload(
        @SerializedName("Attributes") val attributes: Attributes,
    ) : DeviceRegistryRequestPayload() {
        data class Attributes(
            @SerializedName("DeviceName") val deviceName: String,
        )
    }
}

data class DeviceRegistryUpdateTimezoneRequest(
    @Transient @SerializedName("DeviceIdentity") override val deviceIdentity: String,
    @Transient @SerializedName("Payload") override val payload: Payload,
    @Transient @SerializedName("clientToken") override val clientToken: String,
) : DeviceRegistryUpdateRequest(deviceIdentity, payload, clientToken) {
    data class Payload(
        @SerializedName("Attributes") val attributes: Attributes,
    ) : DeviceRegistryRequestPayload() {
        data class Attributes(
            @SerializedName("Timezone") val timezone: Timezone,
        ) {
            data class Timezone(
                @SerializedName("ShortName") val shortName: String,
                @SerializedName("Offset") val offset: Int,
            )
        }
    }
}

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

data class DeviceAccessCodeUpdateRequest(
    @SerializedName("DeviceIdentity") val deviceIdentity: String,
    @SerializedName("CodeType") val codeType: String,
    @SerializedName("AccessCode") val accessCode: List<AccessCode>,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class AccessCode(
        @SerializedName("Name") val name: String,
        @SerializedName("NewCode") val newCode: String,
        @SerializedName("Attributes") val attributes: Attributes,
    ) {
        data class Attributes(
            @SerializedName("NotifyWhenUse") val notifyWhenUse: Boolean,
            @SerializedName("Rule") val rule: List<Rule>,
        ) {
            data class Rule(
                @SerializedName("Type") val type: String,
            )
        }
    }
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