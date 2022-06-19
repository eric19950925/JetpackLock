package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.LockOrientation
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceListResponse


interface SunionIotService {
    suspend fun deviceProvisionCreate(
        idToken: String,
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ): Boolean

    suspend fun getDeviceList(idToken: String, clientToken: String): List<DeviceListResponse.Device>

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

    suspend fun getBoltOrientation(thingName: String, clientToken: String): LockOrientation

    suspend fun lock(idToken: String, thingName: String, clientToken: String)

    suspend fun unlock(idToken: String, thingName: String, clientToken: String)
}