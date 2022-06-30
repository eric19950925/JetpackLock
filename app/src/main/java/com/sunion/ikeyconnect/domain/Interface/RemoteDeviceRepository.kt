package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceListResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceProvisionCreateRequest
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceUpdateResponse

interface RemoteDeviceRepository {
    suspend fun create(request: DeviceProvisionCreateRequest): String

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