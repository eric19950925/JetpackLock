package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.lock.WifiConnectState
import kotlinx.coroutines.flow.Flow

interface SunionWifiService {
    companion object {
        val SERVICE_UUID = "fc3d8cf8-4ddc-7ade-1dd9-2497851131d7"
        val CMD_CHARACTERISTOC_UUID = "de915dce-3539-61ea-ade7-d44a2237601f"
        val CMD_LIST_WIFI = "L"
        val CMD_SET_SSID_PREFIX = "S"
        val CMD_SET_PASSWORD_PREFIX = "P"
        val CMD_CONNECT = "C"
    }

    fun collectWifiList(): Flow<WifiListResult>

    suspend fun scanWifi()

    fun collectConnectToWifiState(): Flow<WifiConnectState>

    suspend fun connectLockToWifi(ssid: String, password: String): Boolean
}


sealed class WifiListResult {
    data class Wifi(val ssid: String, val needPassword: Boolean) : WifiListResult()
    object End : WifiListResult()
}