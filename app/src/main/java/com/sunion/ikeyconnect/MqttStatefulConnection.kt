package com.sunion.ikeyconnect

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobileconnectors.iot.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttStatefulConnection @Inject constructor(
    private val mqttManager: AWSIotMqttManager,
    private val repo: TopicRepositoryImpl,
){
    private val _connectionState = MutableSharedFlow<ConnectMqttUiEvent>()
    val connectionState: SharedFlow<ConnectMqttUiEvent> = _connectionState
    private val topicSubscribedList = mutableListOf<String>()
    private var credentialsProvider: AWSCredentialsProvider? = null
    var isReconnected = false

    fun setCredentialsProvider(awsCredentialsProvider: AWSCredentialsProvider){
        credentialsProvider = awsCredentialsProvider
        runBlocking {_connectionState.emit(ConnectMqttUiEvent.Prepared)}
    }

    fun connectMqtt() {
        val mAWSIotMqttClientStatusCallback =
            AWSIotMqttClientStatusCallback { status, throwable ->
                Timber.d("Mqtt status :$status")
                when(status){
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connecting -> {
                        Timber.d("Mqtt Connecting")
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected -> {
                        Timber.d("Mqtt Connected")
                        runBlocking {
                            _connectionState.emit(ConnectMqttUiEvent.Connected)
                        }
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost -> {
                        Timber.d("Mqtt ConnectionLost")
                        runBlocking {_connectionState.emit(ConnectMqttUiEvent.ConnectionLost)}
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Reconnecting -> {
                        Timber.d("Mqtt Reconnecting")
                        runBlocking {
                            isReconnected = true
                            _connectionState.emit(ConnectMqttUiEvent.Reconnecting)
                        }
                    }
                    else -> {
                        Timber.d(throwable.toString())
                    }
                }

                if(throwable!=null){
                    Timber.d(throwable.toString())
                }
            }

        mqttManager.setAutoResubscribe(true)
        mqttManager.setAutoReconnect(true)
        mqttManager.maxAutoReconnectAttempts = 99
        mqttManager.keepAlive = 15

        mqttManager.connect(credentialsProvider, mAWSIotMqttClientStatusCallback)

    }

    fun pubDeviceList(idToken: String, identityId: String) {
        val payload =
            "{\"API\":\"device-list\",\"RequestBody\":{\"clientToken\":\"AAA\"},\"Authorization\":\"${idToken}\"}"

        mqttManager.publishString(payload, repo.apiPortalTopic(identityId), AWSIotMqttQos.QOS0)
    }

    fun pubGetThingShadow(thingName: String){
        val payload = "{\"clientToken\":\"${thingName}\"}"
        mqttManager.publishString(payload, repo.thingGetTopic(thingName), AWSIotMqttQos.QOS0)
    }

    fun subUpdateThingShadow(thingName: String, callback: AWSIotMqttNewMessageCallback){
        if(topicSubscribedList.contains(repo.thingUpdateDocTopic(thingName)))return
        topicSubscribedList.add(repo.thingUpdateDocTopic(thingName))
        mqttManager.subscribeToTopic(repo.thingUpdateDocTopic(thingName), AWSIotMqttQos.QOS0, callback)
    }

    fun subPubGetThingShadow(thingName: String, callbackForMqtt: AWSIotMqttNewMessageCallback) {
        /** Remember the topics you had subscribed */
        if(!topicSubscribedList.contains(repo.thingGetAcceptedTopic(thingName))){
            topicSubscribedList.add(repo.thingGetAcceptedTopic(thingName))
            topicSubscribedList.add(repo.thingGetRejectedTopic(thingName))
            mqttManager.subscribeToTopic(repo.thingGetAcceptedTopic(thingName), AWSIotMqttQos.QOS0, callbackForPub_GetDeviceShadow(thingName), callbackForMqtt)
            mqttManager.subscribeToTopic(repo.thingGetRejectedTopic(thingName), AWSIotMqttQos.QOS0, callbackForMqtt)
        }
    }

    private fun callbackForPub_GetDeviceShadow(thingName: String) = object: AWSIotMqttSubscriptionStatusCallback{
        override fun onSuccess() {
            Timber.d("sub succ do pub")
            pubGetThingShadow(thingName)
        }

        override fun onFailure(exception: Throwable?) { Timber.e(exception) }
    }

    fun subscribeApiPortal(idToken: String, identityId: String, callbackForMqtt: AWSIotMqttNewMessageCallback) {
        if(!topicSubscribedList.contains(repo.apiPortalAcceptedTopic(identityId))){
            topicSubscribedList.add(repo.apiPortalAcceptedTopic(identityId))
            topicSubscribedList.add(repo.apiPortalRejectedTopic(identityId))
            mqttManager.subscribeToTopic(repo.apiPortalAcceptedTopic(identityId), AWSIotMqttQos.QOS0, callbackForPub_ApiPortal(idToken, identityId), callbackForMqtt)
            mqttManager.subscribeToTopic(repo.apiPortalRejectedTopic(identityId), AWSIotMqttQos.QOS0, callbackForMqtt)
        }
    }
    private fun callbackForPub_ApiPortal(idToken: String, identityId: String) = object: AWSIotMqttSubscriptionStatusCallback{
        override fun onSuccess() {
            Timber.d("sub succ do pub")
            pubDeviceList(idToken, identityId)
        }

        override fun onFailure(exception: Throwable?) { Timber.e(exception) }
    }
}

sealed class ConnectMqttUiEvent {
    object Connected : ConnectMqttUiEvent()
    object Connecting : ConnectMqttUiEvent()
    object ConnectionLost : ConnectMqttUiEvent()
    object Reconnecting : ConnectMqttUiEvent()
    object Prepared : ConnectMqttUiEvent()
}