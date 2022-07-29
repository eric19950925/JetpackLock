package com.sunion.ikeyconnect.domain.model.sunion_service

data class UserSyncResponse (
    val API: String,
    val ResponseBody: UserSyncResponseBody,
)

data class UserSyncResponseBody (
    val DeviceIdentity: String,
    val Filter: FilterInfo,
    val clientToken: String?,
)

data class FilterInfo (
    val BleLockInfo: InfoOfUser?,
    val DeviceOrder: InfoOfUser?,
)

data class InfoOfUser (
    val Data: String,
    val ModifyTime: Int,
)

data class OrderData (
    val DeviceIdentity: String,
    val DeviceType: String,
    val Order: Int,
    val DeviceName: String,
)

