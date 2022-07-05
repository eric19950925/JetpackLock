package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.sunion_service.*

interface RemoteDeviceRepository {
    suspend fun create(request: DeviceProvisionCreateRequest): String

    suspend fun ticketGet(request: DeviceProvisionTicketGetRequest): DeviceProvisionTicketGetResponse

    suspend fun list(clientToken: String): DeviceListResponse

    suspend fun updateDeviceName(
        thingName: String,
        deviceName: String,
        clientToken: String
    ): DeviceUpdateResponse

    suspend fun updateTimezone(
        thingName: String,
        timezone: String,
        clientToken: String
    ): DeviceUpdateResponse

    suspend fun updateAdminCode(
        thingName: String,
        adminCode: String,
        clientToken: String
    ): DeviceUpdateResponse

    suspend fun delete(thingName: String, clientToken: String): String

    suspend fun checkOrientation(thingName: String, clientToken: String)

    suspend fun lock(thingName: String, clientToken: String)

    suspend fun unlock(thingName: String, clientToken: String)
}