package com.sunion.ikeyconnect.domain.model

data class DeviceListResponse (
    val API: String,
    val ResponseBody: DeviceListResponseBody,
)

data class DeviceListResponseBody (
    val Devices: List<DeviceThing>?,
    val ShareDevices: List<DeviceThing>?,
    val Timestamp: Int,
    val clientToken: String?,
)

data class DeviceThing (
    val ThingName: String,
    val Attributes: DeviceAttributes,
)

data class WiFiLock (
    val ThingName: String,
    val Attributes: DeviceAttributes,
    val LockState: Reported,
)

data class DeviceAttributes (
    val Bluetooth: DeviceBleInfo,
    val DeviceName: String,
    val Syncing: Boolean,
    val VacationMode: Boolean,
)

data class DeviceBleInfo (
    val BroadcastName: String,
    val ConnectionKey: String,
    val MACAddress: String,
    val ShareToken: String,
)

