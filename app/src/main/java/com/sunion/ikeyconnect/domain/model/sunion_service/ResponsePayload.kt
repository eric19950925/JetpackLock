package com.sunion.ikeyconnect.domain.model.sunion_service

import com.google.gson.annotations.SerializedName

data class DeviceProvisionCreateResponse(
    @SerializedName("Ticket") val ticket: String,
    @SerializedName("clientToken") val clientToken: String,
)

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