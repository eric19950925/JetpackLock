package com.sunion.ikeyconnect.domain.model

//data class DeviceShadowDoc (
//    val previous: DocLockInfomation,
//    val current: DocLockInfomation,
//    val timestamp: Int,
//    val clientToken: String,
//)

//data class DocLockInfomation (
//    val state: LockState,
//    val metadata: MetaData,
//    val version: Int,
//)

//data class LockInfomation (
//    val state: LockState,
//    val metadata: MetaData,
//    val version: Int,
//    val timestamp: Int,
//    val clientToken: String,
//)
//data class LockState (
//    val desired: Desired,
//    val reported: Reported,
//)
//
//data class MetaData (
//    val desired: DesiredTimestamp,
//    val reported: ReportedTimestamp,
//)

//data class Desired (
//    val RegistryVersion: String,
//    val Deadbolt: String,
//    val UserName: String,
//)
//data class DesiredTimestamp (
//    val RegistryVersion: Timestamp,
//)
//data class Timestamp (
//    val timestamp: Int,
//)

data class Reported (
    val Battery: Int,
    val Rssi: Int,
    val Status: Int,
    val RegistryVersion: Int,
    val AccessCodeTime: Long,
    var Deadbolt: String,
    var Direction: String,
    val Searchable: Int,
    var Connected: Boolean,
)

//data class ReportedTimestamp (
//    val Battery: Timestamp,
//    val Rssi: Timestamp,
//    val Status: Timestamp,
//    val RegistryVersion: Timestamp,
//    val AccessCodeTime: Timestamp,
//    val Deadbolt: Timestamp,
//    val Direction: Timestamp,
//    val Searchable: Timestamp,
//    val Connected: Timestamp,
//)

//data class StateInfo (
//    val welcome: String,
//    val color: String,
//    val power: String,
//)

