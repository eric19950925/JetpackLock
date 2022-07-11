package com.sunion.ikeyconnect.domain.model.sunion_service

import com.google.gson.annotations.SerializedName

data class DeviceProvisionCreateResponse(
    @SerializedName("Ticket") val ticket: String,
    @SerializedName("clientToken") val clientToken: String,
)

data class DeviceProvisionTicketGetResponse(
    @SerializedName("Ticket") val ticket: String,
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

data class DeviceListResponse(
    @SerializedName("Devices") val devices: List<Device>,
    @SerializedName("ShareDevices") val shareDevices: List<ShareDevice>,
    @SerializedName("Timestamp") val timestamp: Int,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Device(
        @SerializedName("ThingName") val thingName: String,
        @SerializedName("Attributes") val attributes: Attributes,
    ) {
        data class Attributes(
            @SerializedName("DeviceName") val deviceName: String,
            @SerializedName("VacationMode") val vacationMode: Boolean,
            @SerializedName("Syncing") val syncing: Boolean,
            @SerializedName("Bluetooth") val bluetooth: Bluetooth,
        ) {
            data class Bluetooth(
                @SerializedName("BroadcastName") val broadcastName: String,
                @SerializedName("MACAddress") val mACAddress: String,
                @SerializedName("ConnectionKey") val connectionKey: String,
                @SerializedName("ShareToken") val shareToken: String,
            )
        }
    }

    data class ShareDevice(
        @SerializedName("ThingName") val thingName: String,
        @SerializedName("Attributes") val attributes: Attributes,
    ) {
        data class Attributes(
            @SerializedName("DeviceName") val deviceName: String,
            @SerializedName("VacationMode") val vacationMode: Boolean,
            @SerializedName("Syncing") val syncing: Boolean,
            @SerializedName("UserRole") val userRole: String,
            @SerializedName("Bluetooth") val bluetooth: Bluetooth,
        ) {
            data class Bluetooth(
                @SerializedName("BroadcastName") val broadcastName: String,
                @SerializedName("MACAddress") val mACAddress: String,
                @SerializedName("ConnectionKey") val connectionKey: String,
                @SerializedName("ShareToken") val shareToken: String,
            )
        }
    }
}

data class DeviceUpdateResponse(
    @SerializedName("version") val version: Int,
    @SerializedName("clientToken") val clientToken: String,
)

data class DeviceShadowUpdateLockResponse(
    @SerializedName("clientToken") val clientToken: String,
    @SerializedName("message") val message: String?,
)

data class EventGetResponse(
    @SerializedName("clientToken") val clientToken: String?,
    @SerializedName("LockGeneral") val lockGeneral: LockGeneral?,
){
    data class LockGeneral(
        @SerializedName("Events") val events: List<Events>?,
    ) {
        data class Events(
            @SerializedName("Type") val Type: String,
            @SerializedName("ExtraDetail") val extraDetail: ExtraDetail?,
            @SerializedName("Millisecond") val Millisecond: Long,
        ){
            data class ExtraDetail(
                @SerializedName("Actor") val Actor: String?,
                @SerializedName("Message") val Message: String?,
            )
        }
    }
}
