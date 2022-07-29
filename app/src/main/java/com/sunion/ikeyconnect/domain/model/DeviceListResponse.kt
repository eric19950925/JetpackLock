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

data class SunionLock (
    val DeviceIdentity: String,
    val Attributes: DeviceAttributes?,
    val LockState: Reported?,
    val BleLockInfo: BleLock?,
    val LockType: Int,
    val Order: Int,
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

data class BleLock (
    val MACAddress: String,
    val DisplayName: String,
    val ConnectionKey: String,
    val OneTimeToken: String,
    val PermanentToken: String,
    val SharedFrom: String,
)

