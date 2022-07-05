package com.sunion.ikeyconnect

class TopicRepositoryImpl {
    fun thingUpdateDocTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update/documents"
    fun thingGetAcceptedTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get/accepted"
    fun thingGetRejectedTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get/rejected"
    fun thingGetTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get"

    fun updateTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal"
    fun updateAcceptedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/accepted"
    fun updateRejectedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/rejected"

    fun apiPortalTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal"
    fun apiPortalAcceptedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/accepted"
    fun apiPortalRejectedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/rejected"
}