package com.sunion.ikeyconnect.domain.model.sunion_service.payload

import com.google.gson.annotations.SerializedName

data class DeviceAccessCodeGetRequest(
    @SerializedName("DeviceIdentity") val deviceIdentity: String,
    @SerializedName("Filter") val filter: Filter,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class Filter(
        @SerializedName("CodeType") val codeType: String,
        @SerializedName("Code") val code: List<String>?,
        @SerializedName("Attributes") val attributes: List<String>,
        @SerializedName("Count") val count: Int,
        @SerializedName("Offset") val offset: Int,
    )
}

data class DeviceAccessCodeGetResponse(
    @SerializedName("CodeType") val codeType: String,
    @SerializedName("AccessCode") val accessCode: List<AccessCode?>,
    @SerializedName("Timestamp") val timestamp: Long,
    @SerializedName("Total") val total: Int,
    @SerializedName("Offset") val offset: Int,
    @SerializedName("clientToken") val clientToken: String,
){
    data class AccessCode(
        @SerializedName("Code") val code: String,
        @SerializedName("Timestamp") val timestamp: Long?,
        @SerializedName("Name") val name: String,
        @SerializedName("Attributes") val attributes: Attributes,
    ) {
        data class Attributes(
            @SerializedName("NotifyWhenUse") val notifyWhenUse: Boolean,
            @SerializedName("Rule") val rule: List<DeviceAccessCodeUpdateResponse.AccessCode.Attributes.Rule>?,
        )
    }
}


data class DeviceAccessCodeUpdateRequest(
    @SerializedName("DeviceIdentity") val deviceIdentity: String,
    @SerializedName("CodeType") val codeType: String,
    @SerializedName("AccessCode") val accessCode: List<AccessCode>,
    @SerializedName("clientToken") val clientToken: String,
) {
    data class AccessCode(
        @SerializedName("Name") val name: String,
        @SerializedName("NewCode") val newCode: String,
        @SerializedName("OldCode") val oldCode: String?,
        @SerializedName("Attributes") val attributes: Attributes,
    ) {
        data class Attributes(
            @SerializedName("NotifyWhenUse") val notifyWhenUse: Boolean,
            @SerializedName("Rule") val rule: List<DeviceAccessCodeUpdateResponse.AccessCode.Attributes.Rule>?,
        )
    }
}
data class DeviceAccessCodeUpdateResponse(
    @SerializedName("DeviceIdentity") val deviceIdentity: String,
    @SerializedName("clientToken") val clientToken: String?,
    @SerializedName("CodeType") val codeType: String,
    @SerializedName("AccessCode") val accessCode: List<AccessCode>?,
){
    data class AccessCode(
        @SerializedName("Name") val name: String,
        @SerializedName("NewCode") val newCode: String,
        @SerializedName("OldCode") val oldCode: String,
        @SerializedName("Attributes") val attributes: Attributes,
    ) {
        data class Attributes(
            @SerializedName("NotifyWhenUse") val notifyWhenUse: Boolean,
            @SerializedName("Rule") val rule: List<Rule>?,
        ) {
            data class Rule(
                @SerializedName("Type") val type: String,
                @SerializedName("Conditions") val conditions: Conditions?,
            ){
                data class Conditions(
                    @SerializedName("Scheduled") val scheduled: Scheduled?,
                    @SerializedName("ValidTimeRange") val validTimeRange: ValidTimeRange?,
                ){
                    data class Scheduled(
                        @SerializedName("WeekDay") val weekDay: String,
                        @SerializedName("StartTime") val startTime: Int,
                        @SerializedName("EndTime") val endTime: Int,
                    )
                    data class ValidTimeRange(
                        @SerializedName("StartTimeStamp") val startTimeStamp: Int,
                        @SerializedName("EndTimeStamp") val endTimeStamp: Int,
                    )
                }
            }
        }
    }
}