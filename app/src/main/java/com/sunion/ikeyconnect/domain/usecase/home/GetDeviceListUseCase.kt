package com.sunion.ikeyconnect.domain.usecase.home

import android.content.Context
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.mobileconnectors.iot.AWSIotMqttSubscriptionStatusCallback
import com.sunion.ikeyconnect.TopicRepositoryImpl
import com.sunion.ikeyconnect.api.APIObject
import com.sunion.ikeyconnect.domain.Interface.MqttSupPubRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDeviceListUseCase @Inject constructor(
    private val mqttManager: AWSIotMqttManager,
    private val repo: TopicRepositoryImpl,
    private val context: Context
): MqttSupPubRepository {

    private val topicSubscribedList = mutableListOf<String>()

    override fun unsubscribeAllTopic() {
        topicSubscribedList.forEach { mqttManager.unsubscribeTopic(it) }
        topicSubscribedList.clear()
        Timber.d("Unsubscribe All Topic of DeviceList.")
    }

    fun pubDeviceList(idToken: String, identityId: String, getUuid: String) {
        val payload =
            "{\"API\":\"${APIObject.DeviceList.route}\",\"RequestBody\":{\"clientToken\":\"${getUuid}\"},\"Authorization\":\"${idToken}\"}"

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
        Timber.d("subGetThingShadow...")
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

    private fun callbackForPub_GetThingShadow(thingName: String, getUuid: String) = object:
        AWSIotMqttSubscriptionStatusCallback {
        override fun onSuccess() {
            Timber.d("===// Sub success do Pub //===")
            pubGetThingShadow(thingName, getUuid)
        }

        override fun onFailure(exception: Throwable?) { Timber.e(exception) }
    }


}