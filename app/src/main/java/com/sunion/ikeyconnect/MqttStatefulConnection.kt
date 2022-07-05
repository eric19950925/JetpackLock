package com.sunion.ikeyconnect

import android.app.Application
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobileconnectors.iot.*
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException
import com.jakewharton.processphoenix.ProcessPhoenix
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
    private val context: Application
){
    private val _connectionState = MutableSharedFlow<ConnectMqttUiEvent>()
    val connectionState: SharedFlow<ConnectMqttUiEvent> = _connectionState
    private val topicSubscribedList = mutableListOf<String>()
    private var credentialsProvider: AWSCredentialsProvider? = null

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
                            unsubscribeAllTopic() // must do after emit
                        }
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost -> {
                        Timber.d("Mqtt ConnectionLost")
                        runBlocking {
                            /** activity stop or destroy.*/
                            _connectionState.emit(ConnectMqttUiEvent.ConnectionLost)
                        }
                    }
                    AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Reconnecting -> {
                        Timber.d("Mqtt Reconnecting")
                        runBlocking {
                            /** wifi disconnect*/
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
        mqttManager.isAutoReconnect = true
        mqttManager.maxAutoReconnectAttempts = 99
        mqttManager.keepAlive = 15

        try {
            mqttManager.connect(credentialsProvider, mAWSIotMqttClientStatusCallback)
        }catch (e: Exception){
            when(e){
                is NotAuthorizedException -> ProcessPhoenix.triggerRebirth(context)
                else -> { Timber.e(e) }
            }
        }

    }

    fun unsubscribeAllTopic() {
        topicSubscribedList.forEach { mqttManager.unsubscribeTopic(it) }
        topicSubscribedList.clear()
        Timber.d("Unsubscribe All Topic.")
    }

    fun pubDeviceList(idToken: String, identityId: String, getUuid: String) {
        val payload =
            "{\"API\":\"device-list\",\"RequestBody\":{\"clientToken\":\"${getUuid}\"},\"Authorization\":\"${idToken}\"}"

        mqttManager.publishString(payload, repo.apiPortalTopic(identityId), AWSIotMqttQos.QOS0)
    }

    fun pubGetThingShadow(thingName: String, getUuid: String){
        val payload = "{\"clientToken\":\"$getUuid\"}"
        mqttManager.publishString(payload, repo.thingGetTopic(thingName), AWSIotMqttQos.QOS0)
    }

    fun subUpdateThingShadow(thingName: String, callback: AWSIotMqttNewMessageCallback){
        if(topicSubscribedList.contains(repo.thingUpdateDocTopic(thingName)))return
        topicSubscribedList.add(repo.thingUpdateDocTopic(thingName))
        mqttManager.subscribeToTopic(repo.thingUpdateDocTopic(thingName), AWSIotMqttQos.QOS0, callback)
    }

    fun subGetThingShadow(
        thingName: String,
        getUuid: String,
        callbackForMqtt: AWSIotMqttNewMessageCallback
    ) {
        /** Remember the topics you had subscribed */
        if(!topicSubscribedList.contains(repo.thingGetAcceptedTopic(thingName))){
            topicSubscribedList.add(repo.thingGetAcceptedTopic(thingName))
            topicSubscribedList.add(repo.thingGetRejectedTopic(thingName))
            mqttManager.subscribeToTopic(repo.thingGetAcceptedTopic(thingName), AWSIotMqttQos.QOS0, callbackForPub_GetThingShadow(thingName, getUuid), callbackForMqtt)
            mqttManager.subscribeToTopic(repo.thingGetRejectedTopic(thingName), AWSIotMqttQos.QOS0
            ) { _, data ->
                val message = String(data?:return@subscribeToTopic, Charsets.UTF_8)
                Timber.d("callbackMqttGetThing Msg: $message")
            }
        }
    }

    private fun callbackForPub_GetThingShadow(thingName: String, getUuid: String) = object: AWSIotMqttSubscriptionStatusCallback{
        override fun onSuccess() {
            Timber.d("===// Sub success do Pub //===")
            pubGetThingShadow(thingName, getUuid)
        }

        override fun onFailure(exception: Throwable?) { Timber.e(exception) }
    }

    fun subscribeApiPortal(idToken: String, identityId: String, getUuid: String, callbackForMqtt: AWSIotMqttNewMessageCallback) {
        if(!topicSubscribedList.contains(repo.apiPortalAcceptedTopic(identityId))){
            topicSubscribedList.add(repo.apiPortalAcceptedTopic(identityId))
            topicSubscribedList.add(repo.apiPortalRejectedTopic(identityId))
            mqttManager.subscribeToTopic(repo.apiPortalAcceptedTopic(identityId), AWSIotMqttQos.QOS0, callbackForPub_ApiPortal(idToken, identityId, getUuid), callbackForMqtt)
            mqttManager.subscribeToTopic(repo.apiPortalRejectedTopic(identityId), AWSIotMqttQos.QOS0) { _, data ->
                val message = String(data?:return@subscribeToTopic, Charsets.UTF_8)
                Timber.d("callbackMqttGetThing Msg: $message")
            }
        }
    }
    private fun callbackForPub_ApiPortal(idToken: String, identityId: String, getUuid: String) = object: AWSIotMqttSubscriptionStatusCallback{
        override fun onSuccess() {
            Timber.d("===// Sub success do Pub //===")
            pubDeviceList(idToken, identityId, getUuid)
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