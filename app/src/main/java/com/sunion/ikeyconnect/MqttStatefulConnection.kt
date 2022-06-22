package com.sunion.ikeyconnect

import androidx.lifecycle.viewModelScope
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.google.gson.Gson
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceShadowUpdateLockRequest
import com.sunion.ikeyconnect.domain.model.sunion_service.RequestPayload
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.UnsupportedEncodingException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttStatefulConnection @Inject constructor(
    private val mqttManager: AWSIotMqttManager,
    private val repo: TopicRepositoryImpl,

    private val gson: Gson
){
    private val _connectionState = MutableSharedFlow<Boolean>()
    val connectionState: SharedFlow<Boolean> = _connectionState
//    private var mqttConnectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun connectMqtt(awsCredentialsProvider: AWSCredentialsProvider) {
        val mAWSIotMqttClientStatusCallback =
            AWSIotMqttClientStatusCallback { status, throwable ->
                when(status){
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connecting -> {
                        Timber.d("Mqtt Connecting")
//                        viewModelScope.launch {
//                        mMqttStatus.value = MqttStatus.CONNECTTING
//                        }
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected -> {
                        Timber.d("Mqtt Connected")
                        runBlocking {_connectionState.emit(true)}
//                        viewModelScope.launch {
//                        mMqttStatus.value = MqttStatus.CONNECTED
//                        }
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost -> {
                        Timber.d("Mqtt ConnectionLost")
//                        viewModelScope.launch {
//                        mMqttStatus.value = MqttStatus.CONNECTION_LOST
//                        }
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Reconnecting -> {
                        Timber.d("Mqtt Reconnecting")
//                        viewModelScope.launch {
//                        mMqttStatus.value = MqttStatus.RECONNECTING
//                        }
                    }
                    else -> {
                        Timber.d(throwable.toString())
//                        viewModelScope.launch {
//                        mMqttStatus.value = MqttStatus.UNCONNECT
//                        }
                    }
                }

                if(throwable!=null){
                    Timber.d(throwable.toString())
                }
            }
        mqttManager.connect(awsCredentialsProvider, mAWSIotMqttClientStatusCallback)
    }

    fun subUpdateThingShadow(thingName: String, callback: AWSIotMqttNewMessageCallback){
        mqttManager.subscribeToTopic(repo.thingUpdateDocTopic(thingName), AWSIotMqttQos.QOS0, callback)
    }

    fun subPubGetDeviceShadow(thingName: String, callbackForMqtt: AWSIotMqttNewMessageCallback) {
        mqttManager.subscribeToTopic(repo.thingGetAcceptedTopic(thingName), AWSIotMqttQos.QOS0, callbackForMqtt)
        mqttManager.subscribeToTopic(repo.thingGetRejectedTopic(thingName), AWSIotMqttQos.QOS0, callbackForMqtt)
        val payload = "{\"clientToken\":\"${thingName}\"}"

        mqttManager.publishString(payload, repo.thingGetTopic(thingName), AWSIotMqttQos.QOS0)
    }

    fun subscribeApiPortal(idToken: String, identityId: String, callbackForMqtt: AWSIotMqttNewMessageCallback) {
        mqttManager.subscribeToTopic(repo.apiPortalAcceptedTopic(identityId), AWSIotMqttQos.QOS0, callbackForMqtt)
        mqttManager.subscribeToTopic(repo.apiPortalRejectedTopic(identityId), AWSIotMqttQos.QOS0, callbackForMqtt)
        val payload =
            "{\"API\":\"device-list\",\"RequestBody\":{\"clientToken\":\"AAA\"},\"Authorization\":\"${idToken}\"}"

        mqttManager.publishString(payload, repo.apiPortalTopic(identityId), AWSIotMqttQos.QOS0)
    }
}