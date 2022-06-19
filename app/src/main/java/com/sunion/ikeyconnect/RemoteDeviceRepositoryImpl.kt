package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.api.DeviceAPI
import com.sunion.ikeyconnect.domain.Interface.RemoteDeviceRepository
import com.sunion.ikeyconnect.domain.model.sunion_service.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteDeviceRepositoryImpl @Inject constructor(
    private val deviceAPI: DeviceAPI
    ) : RemoteDeviceRepository {
    override suspend fun create(idToken: String, request: DeviceProvisionCreateRequest): String =
        deviceAPI.deviceProvisionCreate(idToken = "Bearer $idToken", request = request).clientToken

    override suspend fun list(idToken: String, clientToken: String): DeviceListResponse =
        deviceAPI.deviceList(idToken = "Bearer $idToken", clientToken = clientToken)

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
        clientToken: String
    ): DeviceUpdateResponse = deviceAPI.deviceAccessCodeUpdate(
        DeviceAccessCodeUpdateRequest(
            deviceIdentity = thingName,
            codeType = "AdminCode",
            accessCode = listOf(
                DeviceAccessCodeUpdateRequest.AccessCode(
                    name = "CreateAdmin",
                    newCode = adminCode,
                    attributes = DeviceAccessCodeUpdateRequest.AccessCode.Attributes(
                        notifyWhenUse = true,
                        rule = listOf(DeviceAccessCodeUpdateRequest.AccessCode.Attributes.Rule("A"))
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

    override suspend fun getBoltOrientation(thingName: String, clientToken: String) {
        deviceAPI.deviceShadowUpdateRunCheck(request = DeviceShadowUpdateRunCheckRequest(
            deviceIdentity = thingName,
            desired = DeviceShadowUpdateRunCheckRequest.Desired(
                direction = "run_check",
                searchable = 1650499727),
            clientToken = clientToken
        ))
    }

    override suspend fun lock(idToken: String, thingName: String, clientToken: String) {
        deviceAPI.deviceShadowUpdateLock(
            idToken = "Bearer $idToken",
            request = DeviceShadowUpdateLockRequest(
            deviceIdentity = thingName,
            desired = DeviceShadowUpdateLockRequest.Desired(
                deadbolt = "lock",
//                searchable = 1650499727
            ),
            clientToken = "clientToken")
        )
    }

    override suspend fun unlock(idToken: String, thingName: String, clientToken: String) {
        deviceAPI.deviceShadowUpdateLock(
            idToken = idToken,
            request = DeviceShadowUpdateLockRequest(
            deviceIdentity = thingName,
            desired = DeviceShadowUpdateLockRequest.Desired(
                deadbolt = "unlock",
//                searchable = 1650499727
            ),
            clientToken = "clientToken")
        )
    }
}