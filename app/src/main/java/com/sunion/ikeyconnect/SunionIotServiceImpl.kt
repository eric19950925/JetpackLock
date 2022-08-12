package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.domain.Interface.RemoteDeviceRepository
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.model.sunion_service.*
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.DeviceAccessCodeGetResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.RegistryGetResponse
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncRequestBody
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncResponseBody
import com.sunion.ikeyconnect.domain.usecase.home.GetUserSyncRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SunionIotServiceImpl @Inject constructor(
    private val remoteDeviceRepository: RemoteDeviceRepository
    ) : SunionIotService {

    override suspend fun deviceProvisionCreate(
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ): String {
        return runCatching {
            remoteDeviceRepository.create(
                DeviceProvisionCreateRequest(
                    applicationID = "Sunion_20220617",
                    model = model,
                    serialNumber = serialNumber,
                    deviceName = deviceName,
                    timezone = DeviceProvisionCreateRequest.Timezone(
                        shortName = timeZone,
                        offset = timeZoneOffset
                    ),
                    dataEncryptionKey = "Sunion_20220620",
                    clientToken = clientToken
                )
            )
        }.getOrElse {
            Timber.e(it)
            ""
        }
    }

    override suspend fun deviceProvisionTicketGet(Ticket: String, clientToken: String): DeviceProvisionTicketGetResponse {
        return runCatching {
            remoteDeviceRepository.ticketGet(
                DeviceProvisionTicketGetRequest(
                    Ticket = Ticket,
                    clientToken = clientToken,
                )
            )
        }.getOrElse {
            Timber.e(it)
            DeviceProvisionTicketGetResponse("","","","","",DeviceProvisionTicketGetResponse.Timezone("",0),"","")
        }
    }

    override suspend fun getDeviceList(clientToken: String): List<DeviceListResponse.Device> =
        remoteDeviceRepository.list(clientToken).devices

    override suspend fun getUserSync(request: PubGetUserSyncRequestBody): PubGetUserSyncResponseBody =
        remoteDeviceRepository.getUserSync(request)

    override suspend fun updateUserSync(request: GetUserSyncRequestBody): PubGetUserSyncResponseBody =
        remoteDeviceRepository.updateUserSync(request)

    override suspend fun updateDeviceName(
        thingName: String,
        deviceName: String,
        clientToken: String,
    ) {
        remoteDeviceRepository.updateDeviceName(
            thingName = thingName,
            deviceName = deviceName,
            clientToken = clientToken
        )
    }

    override suspend fun updateTimezone(thingName: String, timezone: String, clientToken: String) {
        remoteDeviceRepository.updateTimezone(
            thingName = thingName,
            timezone = timezone,
            clientToken = clientToken
        )
    }

    override suspend fun updateAdminCode(
        thingName: String,
        adminCode: String,
        oldCode: String,
        clientToken: String,
    ) {
        remoteDeviceRepository.updateAdminCode(
            thingName = thingName,
            adminCode = adminCode,
            oldCode = oldCode,
            clientToken = clientToken
        )
    }

    override suspend fun createAdminCode(
        thingName: String,
        adminCode: String,
        clientToken: String
    ) {
        remoteDeviceRepository.createAdminCode(
            thingName = thingName,
            adminCode = adminCode,
            clientToken = clientToken
        )
    }

    override suspend fun delete(thingName: String, clientToken: String) {
        remoteDeviceRepository.delete(thingName, clientToken)
    }

    override suspend fun checkOrientation(thingName: String, clientToken: String) {
        remoteDeviceRepository.checkOrientation(thingName, clientToken)
    }

    override suspend fun lock(thingName: String, clientToken: String) {
        remoteDeviceRepository.lock(thingName, clientToken)
    }

    override suspend fun unlock(thingName: String, clientToken: String) {
        remoteDeviceRepository.unlock(thingName, clientToken)
    }

    override suspend fun getEvent(thingName: String, timestamp: Int, clientToken: String): EventGetResponse {
        return remoteDeviceRepository.getEvent(thingName, timestamp, clientToken)
    }

    override suspend fun getDeviceRegistry(
        thingName: String,
        clientToken: String
    ): RegistryGetResponse {
        return remoteDeviceRepository.getRegistry(thingName, clientToken)
    }

    override suspend fun updateDeviceRegistry(
        thingName: String,
        registryAttributes: RegistryGetResponse.RegistryPayload.RegistryAttributes,
        clientToken: String
    ): DeviceUpdateResponse {
        return remoteDeviceRepository.updateDeviceRegistry(thingName, registryAttributes, clientToken)
    }

    override suspend fun updateWiFiSetting(
        thingName: String,
        clientToken: String
    ): DeviceUpdateResponse {
        TODO("Not yet implemented")
    }

    override suspend fun updateLocation(
        thingName: String,
        latitude: Double,
        longitude: Double,
        clientToken: String
    ): DeviceUpdateResponse {
        return remoteDeviceRepository.updateLocation(thingName, latitude, longitude, clientToken)
    }

    override suspend fun updateAutoLock(
        thingName: String,
        enable: Boolean,
        delay: Int,
        clientToken: String
    ): DeviceUpdateResponse {
        return remoteDeviceRepository.updateAutoLock(thingName, enable, delay, clientToken)
    }

    override suspend fun updateAutoLockDelay(
        thingName: String,
        clientToken: String
    ): DeviceUpdateResponse {
        TODO("Not yet implemented")
    }

    override suspend fun updateStatusNotification(
        thingName: String,
        clientToken: String
    ): DeviceUpdateResponse {
        TODO("Not yet implemented")
    }

    override suspend fun updateVacationMode(
        thingName: String,
        enable: Boolean,
        clientToken: String
    ): DeviceUpdateResponse {
        return remoteDeviceRepository.updateVacationMode(thingName, enable, clientToken)
    }

    override suspend fun updateKeyPressBeep(
        thingName: String,
        enable: Boolean,
        clientToken: String
    ): DeviceUpdateResponse {
        return remoteDeviceRepository.updateKeyPressBeep(thingName, enable, clientToken)
    }

    override suspend fun updateOfflineNotifiy(
        thingName: String,
        clientToken: String
    ): DeviceUpdateResponse {
        TODO("Not yet implemented")
    }

    override suspend fun updatePreamble(
        thingName: String,
        enable: Boolean,
        clientToken: String
    ): DeviceUpdateResponse {
        return remoteDeviceRepository.updateVacationMode(thingName, enable, clientToken)
    }

    override suspend fun updateSecureMode(
        thingName: String,
        enable: Boolean,
        clientToken: String
    ): DeviceUpdateResponse {
        return remoteDeviceRepository.updateSecureMode(thingName, enable, clientToken)
    }

    override suspend fun updateSyncing(
        thingName: String,
        clientToken: String
    ): DeviceUpdateResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getAdminCode(
        thingName: String,
        clientToken: String
    ): DeviceAccessCodeGetResponse {
        return remoteDeviceRepository.getAdminCode(thingName, clientToken)
    }
}