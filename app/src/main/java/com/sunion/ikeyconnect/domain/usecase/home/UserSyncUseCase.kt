package com.sunion.ikeyconnect.domain.usecase.home

import androidx.compose.runtime.mutableStateOf
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.google.gson.Gson
import com.sunion.ikeyconnect.TopicRepositoryImpl
import com.sunion.ikeyconnect.api.APIObject
import com.sunion.ikeyconnect.domain.model.BleLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserSyncUseCase @Inject constructor(
    private val mqttManager: AWSIotMqttManager,
    private val repo: TopicRepositoryImpl,
    private val gson: Gson,
) {

    private val modifierVersion = mutableStateOf<Int>(0)

    fun pubGetUserSync(idToken: String, identityId: String, getUuid: String){
        val payload = gson.toJson(
            ApiPortalRequestPayload(
                API = APIObject.GetUserSync.route,
                RequestBody = PubGetUserSyncRequestBody(
                    Filter = UserSyncFilter(listOf("DeviceOrder", "BLEDevices")),
                    clientToken = getUuid
                ),
                Authorization = idToken,
            )
        ).toString()
        Timber.d(payload)

//        val payload2 =
//            "{\"API\":\"${APIObject.UpdateUserSync.route}\",\"RequestBody\":{\"clientToken\":\"${getUuid}\",\"Filter\":{}},\"Authorization\":\"${idToken}\"}"

        mqttManager.publishString(payload, repo.apiPortalTopic(identityId), AWSIotMqttQos.QOS0)
    }

    fun updateModifyVersion(version: Int){
        modifierVersion.value = version
    }

    /**
     * 必須把原有清單保留，並加上新裝置資訊，以及version+1，才能update上去
     */
    fun pubUpdateUserSyncExample(idToken: String, identityId: String, getUuid: String, bleDeviceInfo: String, deviceOrder: String){
        val payload = gson.toJson(
            ApiPortalResponsePayload(
                API = APIObject.UpdateUserSync.route,
                RequestBody = PubGetUserSyncResponseBody(
                    Payload = ResponsePayload(
                        Dataset = ResponseDataset(
                            DeviceOrder = ResponseOrder(
                                listOf(
                                    UserSyncOrder(DeviceIdentity = "6fee53ef-61b5-4e42-ac8d-e027c75c8fed", DeviceType = "wifi", DisplayName = "Lock name1", Order = 1),
                                    UserSyncOrder(DeviceIdentity = "002", DeviceType = "ble mode", DisplayName = "Lock name2", Order = 2),
                                    UserSyncOrder(DeviceIdentity = "003", DeviceType = "ble", DisplayName = "Lock name3", Order = 3),
                                    UserSyncOrder(DeviceIdentity = "004", DeviceType = "ble", DisplayName = "Lock name4", Order = 4),
                                ),
                                expectedVersion = 18
                            ),
                            BLEDevices = ResponseDevices(
                                listOf(
                                    BleLock(MACAddress = "002", DisplayName = "Lock name2", OneTimeToken = "", PermanentToken = "", ConnectionKey = "", SharedFrom = ""),
                                    BleLock(MACAddress = "003", DisplayName = "Lock name3", OneTimeToken = "", PermanentToken = "", ConnectionKey = "", SharedFrom = ""),
                                    BleLock(MACAddress = "004", DisplayName = "Lock name4", OneTimeToken = "", PermanentToken = "", ConnectionKey = "", SharedFrom = ""),
                                ),
                                expectedVersion = 0
                            )
                        )
                    ),
                    clientToken = getUuid
                ),
                Authorization = idToken,
            )
        ).toString()
        Timber.d(payload)

        mqttManager.publishString(payload, repo.apiPortalTopic(identityId), AWSIotMqttQos.QOS0)
        modifierVersion.value ++
    }
    fun updateOrderList(idToken: String, identityId: String, getUuid: String, list: List<UserSyncOrder>){
        val payload = gson.toJson(
            ApiPortalResponsePayload(
                API = APIObject.UpdateUserSync.route,
                RequestBody = PubGetUserSyncResponseBody(
                    Payload = ResponsePayload(
                        Dataset = ResponseDataset(
                            DeviceOrder = ResponseOrder(
                                list,
                                expectedVersion = modifierVersion.value
                            ),
//                            BLEDevices = ResponseDevices(
//                                listOf(
//                                    UserSyncDevices(MACAddress = , DisplayName = , OneTimeToken = , PermanentToken = , ConnectionKey = , SharedFrom = ),
//                                )
//                            )
                        )
                    ),
                    clientToken = getUuid
                ),
                Authorization = idToken,
            )
        ).toString()
        Timber.d(payload)

        mqttManager.publishString(payload, repo.apiPortalTopic(identityId), AWSIotMqttQos.QOS0)
        modifierVersion.value ++
    }

}

data class ApiPortalRequestPayload(
    val API: String,
    val RequestBody: PubGetUserSyncRequestBody,
    val Authorization: String,
)
data class PubGetUserSyncRequestBody(
    val Filter: UserSyncFilter,
    val clientToken: String,
)
data class UserSyncFilter(
    val Dataset: List<String>
)
/**=====================================================================================================*/
data class ApiPortalResponsePayload(
    val API: String,
    val RequestBody: PubGetUserSyncResponseBody,
    val Authorization: String,
)
data class PubGetUserSyncResponseBody(
    val Payload: ResponsePayload,
    val clientToken: String,
)
data class ResponsePayload(
    val Dataset: ResponseDataset
)
data class ResponseDataset(
    val DeviceOrder: ResponseOrder ?= null,
    val BLEDevices: ResponseDevices ?= null,
)
data class ResponseOrder(
    val Order: List<UserSyncOrder>,
    val expectedVersion: Int,
)
data class UserSyncOrder(
    val DeviceIdentity: String,
    val DeviceType: String,
    var Order: Int,
    val DisplayName: String,
)
data class ResponseDevices(
    val Devices: List<BleLock>,
    val expectedVersion: Int,
)
data class UserSyncDevices(
    val MACAddress: String,
    val DisplayName: String,
    val OneTimeToken: String,
    val PermanentToken: String,
    val ConnectionKey: String,
    val SharedFrom: String,
)
