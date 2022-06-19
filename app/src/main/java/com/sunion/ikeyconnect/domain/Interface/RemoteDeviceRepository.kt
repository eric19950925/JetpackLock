package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceListResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceProvisionCreateRequest
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceUpdateResponse

interface RemoteDeviceRepository {
    suspend fun create(idToken: String, request: DeviceProvisionCreateRequest): String

    suspend fun list(idToken: String, clientToken: String): DeviceListResponse

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

    suspend fun getBoltOrientation(thingName: String, clientToken: String)

    suspend fun lock(idToken: String, thingName: String, clientToken: String)

    suspend fun unlock(idToken: String, thingName: String, clientToken: String)
}