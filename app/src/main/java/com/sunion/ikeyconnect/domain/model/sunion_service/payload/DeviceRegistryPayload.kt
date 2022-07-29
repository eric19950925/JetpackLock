package com.sunion.ikeyconnect.domain.model.sunion_service.payload

import com.google.gson.annotations.SerializedName



data class RegistryGetResponse(
    @SerializedName("clientToken") val clientToken: String,
    @SerializedName("version") val version: Int,
    @SerializedName("Payload") val payload: RegistryPayload,
){
    data class RegistryPayload(
        @SerializedName("Attributes") val attributes: RegistryAttributes,
    ) {
        data class RegistryAttributes(
            @SerializedName("AutoLock") val autoLock: Boolean,
            @SerializedName("AutoLockDelay") val autoLockDelay: Int,
            @SerializedName("DeviceName") val deviceName: String,
            @SerializedName("Preamble") val preamble: Boolean,
            @SerializedName("SecureMode") val secureMode: Boolean,
            @SerializedName("Syncing") val syncing: Boolean,
            @SerializedName("Model") val model: String,
            @SerializedName("FirmwareVersion") val firmwareVersion: String,
            @SerializedName("KeyPressBeep") val keyPressBeep: Boolean,
            @SerializedName("OfflineNotifiy") val offlineNotifiy: Boolean,
            @SerializedName("StatusNotification") val statusNotification: Boolean,
            @SerializedName("Timezone") val timezone: Timezone,
            @SerializedName("VacationMode") val vacationMode: Boolean,
            @SerializedName("WiFi") val wifi: WiFiInfo,
            @SerializedName("Bluetooth") val bluetooth: Bluetooth?,

        ){
            data class Timezone(
                @SerializedName("Offset") val offset: Int,
                @SerializedName("ShortName") val shortName: String,
            )
            data class WiFiInfo(
                @SerializedName("Passphrase") val passphrase: String,
                @SerializedName("SSID") val SSID: String,
                @SerializedName("Security") val security: String,
            )
            data class Bluetooth(
                @SerializedName("BroadcastName") val broadcastName: String,
                @SerializedName("MACAddress") val macAddress: String,
                @SerializedName("ConnectionKey") val connectionKey: String,
                @SerializedName("ShareToken") val shareToken: String,
            )
        }
    }
}
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

data class DeviceRegistryUpdateVacationRequest(
    @Transient @SerializedName("DeviceIdentity") override val deviceIdentity: String,
    @Transient @SerializedName("Payload") override val payload: Payload,
    @Transient @SerializedName("clientToken") override val clientToken: String,
) : DeviceRegistryUpdateRequest(deviceIdentity, payload, clientToken) {
    data class Payload(
        @SerializedName("Attributes") val attributes: Attributes,
    ) : DeviceRegistryRequestPayload() {
        data class Attributes(
            @SerializedName("VacationMode") val vacationMode: Boolean,
        )
    }
}

data class DeviceRegistryUpdateKeyPressBeepRequest(
    @Transient @SerializedName("DeviceIdentity") override val deviceIdentity: String,
    @Transient @SerializedName("Payload") override val payload: Payload,
    @Transient @SerializedName("clientToken") override val clientToken: String,
) : DeviceRegistryUpdateRequest(deviceIdentity, payload, clientToken) {
    data class Payload(
        @SerializedName("Attributes") val attributes: Attributes,
    ) : DeviceRegistryRequestPayload() {
        data class Attributes(
            @SerializedName("KeyPressBeep") val keyPressBeep: Boolean,
        )
    }
}

data class DeviceRegistryUpdateSecureModeRequest(
    @Transient @SerializedName("DeviceIdentity") override val deviceIdentity: String,
    @Transient @SerializedName("Payload") override val payload: Payload,
    @Transient @SerializedName("clientToken") override val clientToken: String,
) : DeviceRegistryUpdateRequest(deviceIdentity, payload, clientToken) {
    data class Payload(
        @SerializedName("Attributes") val attributes: Attributes,
    ) : DeviceRegistryRequestPayload() {
        data class Attributes(
            @SerializedName("ScureMode") val scureMode: Boolean,
        )
    }
}

data class DeviceRegistryUpdateAutoLockRequest(
    @Transient @SerializedName("DeviceIdentity") override val deviceIdentity: String,
    @Transient @SerializedName("Payload") override val payload: Payload,
    @Transient @SerializedName("clientToken") override val clientToken: String,
) : DeviceRegistryUpdateRequest(deviceIdentity, payload, clientToken) {
    data class Payload(
        @SerializedName("Attributes") val attributes: Attributes,
    ) : DeviceRegistryRequestPayload() {
        data class Attributes(
            @SerializedName("AutoLock") val autoLock: Boolean,
            @SerializedName("AutoLockDelay") val autoLockDelay: Int,
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

data class DeviceRegistryUpdateSettingRequest(
    @Transient @SerializedName("DeviceIdentity") override val deviceIdentity: String,
    @Transient @SerializedName("Payload") override val payload: Payload,
    @Transient @SerializedName("clientToken") override val clientToken: String,
) : DeviceRegistryUpdateRequest(deviceIdentity, payload, clientToken) {
    data class Payload(
        @SerializedName("Attributes") val attributes: Attributes,
    ) : DeviceRegistryRequestPayload() {
        data class Attributes(
            @SerializedName("AutoLock") val autoLock: Boolean,
            @SerializedName("AutoLockDelay") val autoLockDelay: Int,
            @SerializedName("ScureMode") val scureMode: Boolean,
            @SerializedName("KeyPressBeep") val keyPressBeep: Boolean,
            @SerializedName("VacationMode") val vacationMode: Boolean,
            @SerializedName("Preamble") val preamble: Boolean,
        )
    }
}

data class RegistryGetRequest(
    @SerializedName("DeviceIdentity") val DeviceIdentity: String,
    @SerializedName("Filter") val filter: Filter,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Filter(
        @SerializedName("Attribute") val attribute: List<String>,
        @SerializedName("Group") val group: Boolean,
    )
}
