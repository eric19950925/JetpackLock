package com.sunion.ikeyconnect

import android.app.Application
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobileconnectors.iot.*
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException
import com.jakewharton.processphoenix.ProcessPhoenix
import com.sunion.ikeyconnect.domain.Interface.MqttSupPubRepository
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
): MqttSupPubRepository {
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
                             // must do after emit
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

    fun subscribeApiPortal(
        identityId: String,
        callbackForMqtt: AWSIotMqttNewMessageCallback,
        callbackForPub: AWSIotMqttSubscriptionStatusCallback
    ) {
        if(!topicSubscribedList.contains(repo.apiPortalAcceptedTopic(identityId))){
            topicSubscribedList.add(repo.apiPortalAcceptedTopic(identityId))
            topicSubscribedList.add(repo.apiPortalRejectedTopic(identityId))
            try {
                mqttManager.subscribeToTopic(repo.apiPortalAcceptedTopic(identityId), AWSIotMqttQos.QOS0, callbackForPub, callbackForMqtt)
                mqttManager.subscribeToTopic(repo.apiPortalRejectedTopic(identityId), AWSIotMqttQos.QOS0) { _, data ->
                    val message = String(data?:return@subscribeToTopic, Charsets.UTF_8)
                    Timber.e("ApiPortalRejected: $message")
                }
            }catch (e:Exception){
               Timber.e(e)
            }
        }
    }

    override fun unsubscribeAllTopic() {
        topicSubscribedList.forEach { mqttManager.unsubscribeTopic(it) }
        topicSubscribedList.clear()
        Timber.d("Unsubscribe All Topic of MqttStatefulConnection.")
    }

}

sealed class ConnectMqttUiEvent {
    object Connected : ConnectMqttUiEvent()
    object Connecting : ConnectMqttUiEvent()
    object ConnectionLost : ConnectMqttUiEvent()
    object Reconnecting : ConnectMqttUiEvent()
    object Prepared : ConnectMqttUiEvent()
}