package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.sunion_service.*
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.DeviceAccessCodeGetResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.RegistryGetResponse
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncRequestBody
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncResponseBody
import com.sunion.ikeyconnect.domain.usecase.home.GetUserSyncRequestBody

interface RemoteDeviceRepository {
    suspend fun create(request: DeviceProvisionCreateRequest): String

    suspend fun ticketGet(request: DeviceProvisionTicketGetRequest): DeviceProvisionTicketGetResponse

    suspend fun list(clientToken: String): DeviceListResponse

    suspend fun getUserSync(request: PubGetUserSyncRequestBody): PubGetUserSyncResponseBody

    suspend fun updateUserSync(request: GetUserSyncRequestBody): PubGetUserSyncResponseBody

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
        oldCode: String,
        clientToken: String
    ): DeviceUpdateResponse

    suspend fun createAdminCode(
        thingName: String,
        adminCode: String,
        clientToken: String
    ): DeviceUpdateResponse

    suspend fun delete(thingName: String, clientToken: String): String

    suspend fun checkOrientation(thingName: String, clientToken: String)

    suspend fun lock(thingName: String, clientToken: String)

    suspend fun unlock(thingName: String, clientToken: String)

    suspend fun getEvent(thingName: String, timestamp: Int, clientToken: String): EventGetResponse

    suspend fun getRegistry(thingName: String, clientToken: String): RegistryGetResponse

    suspend fun updateDeviceRegistry(thingName: String, registryAttributes: RegistryGetResponse.RegistryPayload.RegistryAttributes, clientToken: String): DeviceUpdateResponse

    suspend fun updateWiFiSetting(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updateAutoLock(thingName: String, enable: Boolean, delay: Int, clientToken: String): DeviceUpdateResponse

    suspend fun updateAutoLockDelay(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updateStatusNotification(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updateVacationMode(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateLocation(thingName: String, latitude: Double, longitude: Double, clientToken: String): DeviceUpdateResponse

    suspend fun updateKeyPressBeep(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateOfflineNotifiy(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updatePreamble(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateSecureMode(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateSyncing(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun getAdminCode(thingName: String, clientToken: String): DeviceAccessCodeGetResponse
}