package com.sunion.ikeyconnect.domain.model

data class DeviceShadow (
    val state: DeviceState
)

data class DeviceState (
    val DeviceIdentity: String,
    val Desired: StateInfo,
    val clientToken: String,
//    val reported: StateInfo,
//    val delta: StateInfo,
)

data class StateInfo (
    val Deadbolt: String,
    val Direction: String,
    val Searchable: Int,
)

//data class StateInfo (
//    val welcome: String,
//    val color: String,
//    val power: String,
//)

