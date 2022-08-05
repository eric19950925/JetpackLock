package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.api.DeviceAPI
import com.sunion.ikeyconnect.domain.Interface.RemoteDeviceRepository
import com.sunion.ikeyconnect.domain.model.sunion_service.*
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.*
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncRequestBody
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncResponseBody
import com.sunion.ikeyconnect.domain.usecase.home.ResponseDataset
import com.sunion.ikeyconnect.domain.usecase.home.GetUserSyncRequestBody
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteDeviceRepositoryImpl @Inject constructor(
    private val deviceAPI: DeviceAPI
    ) : RemoteDeviceRepository {
    override suspend fun create(request: DeviceProvisionCreateRequest): String =
        deviceAPI.deviceProvisionCreate(request = request).ticket

    override suspend fun ticketGet(request: DeviceProvisionTicketGetRequest): DeviceProvisionTicketGetResponse =
        deviceAPI.deviceProvisionTicketGet(request = request)


    override suspend fun list(clientToken: String): DeviceListResponse =
        deviceAPI.deviceList(clientToken = clientToken)

    override suspend fun getUserSync(request: PubGetUserSyncRequestBody): PubGetUserSyncResponseBody =
        deviceAPI.userSyncGet(request = request)

    override suspend fun updateUserSync(request: GetUserSyncRequestBody): PubGetUserSyncResponseBody =
        deviceAPI.userSyncUpdate(request = request)

    override suspend fun updateDeviceName(
        thingName: String,
        deviceName: String,
        clientToken: String
    ): DeviceUpdateResponse = deviceAPI.deviceRegistryUpdate(
        DeviceRegistryUpdateNameRequest(
            deviceIdentity = thingName,
            payload = DeviceRegistryUpdateNameRequest.Payload(
                attributes = DeviceRegistryUpdateNameRequest.Payload.Attributes(deviceName = deviceName)
            ),
            clientToken = clientToken
        )
    )

    override suspend fun updateTimezone(
        thingName: String,
        timezone: String,
        clientToken: String
    ): DeviceUpdateResponse {
        return deviceAPI.deviceRegistryUpdate(
            DeviceRegistryUpdateTimezoneRequest(
                deviceIdentity = thingName,
                payload = DeviceRegistryUpdateTimezoneRequest.Payload(
                    attributes = DeviceRegistryUpdateTimezoneRequest.Payload.Attributes(
                        timezone = DeviceRegistryUpdateTimezoneRequest.Payload.Attributes.Timezone(
                            shortName = timezone,
                            offset = TimeZone.getTimeZone(timezone)
                                .getOffset(System.currentTimeMillis()) / 1000
                        )
                    )
                ),
                clientToken = clientToken
            )
        )
    }

    override suspend fun updateAdminCode(
        thingName: String,
        adminCode: String,
        oldCode: String,
        clientToken: String
    ): DeviceUpdateResponse = deviceAPI.deviceAccessCodeUpdate(
        DeviceAccessCodeUpdateRequest(
            deviceIdentity = thingName,
            codeType = "AdminCode",
            accessCode = listOf(
                DeviceAccessCodeUpdateRequest.AccessCode(
                    name = "CreateAdmin",
                    newCode = adminCode,
                    oldCode = oldCode,
                    attributes = DeviceAccessCodeUpdateRequest.AccessCode.Attributes(
                        notifyWhenUse = true,
                        rule = listOf(DeviceAccessCodeUpdateResponse.AccessCode.Attributes.Rule("A",null))
                    )
                )
            ),
            clientToken = clientToken
        )
    )

    override suspend fun delete(thingName: String, clientToken: String): String =
        deviceAPI.deviceProvisionDelete(
            DeviceProvisionDeleteRequest(
                deviceIdentity = thingName,
                clientToken = clientToken
            )
        ).clientToken

    override suspend fun checkOrientation(thingName: String, clientToken: String) {
        deviceAPI.deviceShadowUpdateRunCheck(request = DeviceShadowUpdateRunCheckRequest(
            deviceIdentity = thingName,
            desired = DeviceShadowUpdateRunCheckRequest.Desired(
                direction = "run_check",
//                searchable = 1650499727
               ),
            clientToken = clientToken
        ))
    }

    override suspend fun lock(thingName: String, clientToken: String) {
        deviceAPI.deviceShadowUpdateLock(
            request = DeviceShadowUpdateLockRequest(
            deviceIdentity = thingName,
            desired = DeviceShadowUpdateLockRequest.Desired(
                deadbolt = "lock",
//                searchable = 1650499727
            ),
            clientToken = clientToken)
        )
    }

    override suspend fun unlock(thingName: String, clientToken: String) {
        deviceAPI.deviceShadowUpdateLock(
            request = DeviceShadowUpdateLockRequest(
            deviceIdentity = thingName,
            desired = DeviceShadowUpdateLockRequest.Desired(
                deadbolt = "unlock",
//                searchable = 1650499727
            ),
            clientToken = clientToken)
        )
    }

    override suspend fun getEvent(thingName: String, timestamp: Int, clientToken: String): EventGetResponse {
        return deviceAPI.eventGet(
            EventGetRequest(
                DeviceIdentity = thingName,
                filter = EventGetRequest.Filter(
                    TimePoint = timestamp,
                    Maximum = 100
                ),
                clientToken = clientToken
            )
        )
    }

    override suspend fun getRegistry(thingName: String, clientToken: String): RegistryGetResponse {
        return deviceAPI.deviceRegistryGet(
            RegistryGetRequest(
                DeviceIdentity = thingName,
                filter = RegistryGetRequest.Filter(
                    attribute = listOf(
                        "WiFi",
                        "Bluetooth",
                        "Timezone",
                        "DeviceName",
                        "AutoLock",
                        "AutoLockDelay",
                        "StatusNotification",
                        "VacationMode",
                        "KeyPressBeep",
                        "OfflineNotifiy",
                        "Preamble",
                        "SecureMode",
                        "Syncing",
                        "Model",
                        "FirmwareVersion"
                    ),
                    group = false
                ),
                clientToken = clientToken
            )
        )
    }

    override suspend fun updateDeviceRegistry(
        thingName: String,
        registryAttributes: RegistryGetResponse.RegistryPayload.RegistryAttributes,
        clientToken: String
    ): DeviceUpdateResponse {
        return deviceAPI.deviceRegistryUpdate(
            DeviceRegistryUpdateSettingRequest(
                deviceIdentity = thingName,
                payload = DeviceRegistryUpdateSettingRequest.Payload(
                    attributes = DeviceRegistryUpdateSettingRequest.Payload.Attributes(
                        autoLock = registryAttributes.autoLock,
                        autoLockDelay = registryAttributes.autoLockDelay,
                        keyPressBeep = registryAttributes.keyPressBeep,
                        scureMode = registryAttributes.secureMode,
                        preamble = registryAttributes.preamble,
                        vacationMode = registryAttributes.vacationMode,
                    )
                ),
                clientToken = clientToken
            )
        )

    }

    override suspend fun updateWiFiSetting(
        thingName: String,
        clientToken: String
    ): DeviceUpdateResponse {
        TODO("Not yet implemented")

    }

    override suspend fun updateAutoLock(
        thingName: String,
        enable: Boolean,
        delay: Int,
        clientToken: String
    ): DeviceUpdateResponse {
        return deviceAPI.deviceRegistryUpdate(
            DeviceRegistryUpdateAutoLockRequest(
                deviceIdentity = thingName,
                payload = DeviceRegistryUpdateAutoLockRequest.Payload(
                    attributes = DeviceRegistryUpdateAutoLockRequest.Payload.Attributes(autoLock = enable, autoLockDelay = delay)
                ),
                clientToken = clientToken
            )
        )
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
        return deviceAPI.deviceRegistryUpdate(
            DeviceRegistryUpdateVacationRequest(
                deviceIdentity = thingName,
                payload = DeviceRegistryUpdateVacationRequest.Payload(
                    attributes = DeviceRegistryUpdateVacationRequest.Payload.Attributes(vacationMode = enable)
                ),
                clientToken = clientToken
            )
        )
    }

    override suspend fun updateKeyPressBeep(
        thingName: String,
        enable: Boolean,
        clientToken: String
    ): DeviceUpdateResponse {
        return deviceAPI.deviceRegistryUpdate(
            DeviceRegistryUpdateKeyPressBeepRequest(
                deviceIdentity = thingName,
                payload = DeviceRegistryUpdateKeyPressBeepRequest.Payload(
                    attributes = DeviceRegistryUpdateKeyPressBeepRequest.Payload.Attributes(keyPressBeep = enable)
                ),
                clientToken = clientToken
            )
        )
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
        TODO("Not yet implemented")
    }

    override suspend fun updateSecureMode(
        thingName: String,
        enable: Boolean,
        clientToken: String
    ): DeviceUpdateResponse {
        return deviceAPI.deviceRegistryUpdate(
            DeviceRegistryUpdateSecureModeRequest(
                deviceIdentity = thingName,
                payload = DeviceRegistryUpdateSecureModeRequest.Payload(
                    attributes = DeviceRegistryUpdateSecureModeRequest.Payload.Attributes(scureMode = enable)
                ),
                clientToken = clientToken
            )
        )
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
    ): DeviceAccessCodeGetResponse{
        return deviceAPI.deviceAccessCodeGet(
            DeviceAccessCodeGetRequest(
                deviceIdentity = thingName,
                filter = DeviceAccessCodeGetRequest.Filter(
                    codeType = "AdminCode",
                    count = 1,
                    attributes = listOf(
                        "NotifyWhenUse",
                        "Rule",
                        "Name",
                        "Code",
                        "Timestamp"),
                    offset = 0,
                    code = null,
                ),
                clientToken = clientToken
            )
        )
    }

}