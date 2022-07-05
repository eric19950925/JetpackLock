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
    override suspend fun create(request: DeviceProvisionCreateRequest): String =
        deviceAPI.deviceProvisionCreate(request = request).ticket

    override suspend fun ticketGet(request: DeviceProvisionTicketGetRequest): DeviceProvisionTicketGetResponse =
        deviceAPI.deviceProvisionTicketGet(request = request)


    override suspend fun list(clientToken: String): DeviceListResponse =
        deviceAPI.deviceList(clientToken = clientToken)

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
}