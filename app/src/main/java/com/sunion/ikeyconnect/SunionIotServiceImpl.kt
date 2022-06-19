package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.domain.Interface.RemoteDeviceRepository
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.model.LockOrientation
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceListResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceProvisionCreateRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SunionIotServiceImpl @Inject constructor(
    private val remoteDeviceRepository: RemoteDeviceRepository
    ) : SunionIotService {

    override suspend fun deviceProvisionCreate(
        idToken: String,
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ): Boolean {
        return runCatching {
            remoteDeviceRepository.create(
                idToken,
                DeviceProvisionCreateRequest(
                    applicationID = "Sunion_20220620",
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
            ) == clientToken
        }.getOrElse {
            Timber.e(it)
            false
        }
    }

    override suspend fun getDeviceList(idToken: String, clientToken: String): List<DeviceListResponse.Device> =
        remoteDeviceRepository.list(idToken, clientToken).devices

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
        timezone: String,
        clientToken: String,
    ) {
        remoteDeviceRepository.updateAdminCode(
            thingName = thingName,
            adminCode = adminCode,
            clientToken = clientToken
        )
    }

    override suspend fun delete(thingName: String, clientToken: String) {
        remoteDeviceRepository.delete(thingName, clientToken)
    }

    override suspend fun getBoltOrientation(
        thingName: String,
        clientToken: String,
    ): LockOrientation {
        remoteDeviceRepository.getBoltOrientation(thingName, clientToken)
        return LockOrientation.Left//TODO
    }

    override suspend fun lock(idToken: String, thingName: String, clientToken: String) {
        remoteDeviceRepository.lock(idToken, thingName, clientToken)
    }

    override suspend fun unlock(idToken: String, thingName: String, clientToken: String) {
        remoteDeviceRepository.unlock(idToken, thingName, clientToken)
    }
}