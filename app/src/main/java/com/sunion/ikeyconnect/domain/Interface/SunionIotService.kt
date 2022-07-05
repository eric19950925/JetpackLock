package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceListResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceProvisionTicketGetResponse


interface SunionIotService {
    suspend fun deviceProvisionCreate(
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ): String

    suspend fun deviceProvisionTicketGet(
        Ticket: String,
        clientToken: String,
    ): DeviceProvisionTicketGetResponse

    suspend fun getDeviceList(clientToken: String): List<DeviceListResponse.Device>

    suspend fun updateDeviceName(
        thingName: String,
        deviceName: String,
        clientToken: String,
    )

    suspend fun updateTimezone(
        thingName: String,
        timezone: String,
        clientToken: String,
    )

    suspend fun updateAdminCode(
        thingName: String,
        adminCode: String,
        timezone: String,
        clientToken: String,
    )

    suspend fun delete(thingName: String, clientToken: String)

    suspend fun checkOrientation(thingName: String, clientToken: String)

    suspend fun lock(thingName: String, clientToken: String)

    suspend fun unlock(thingName: String, clientToken: String)
}