package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.command.WifiConnectState
import com.sunion.ikeyconnect.domain.model.LockInfo
import kotlinx.coroutines.flow.Flow

interface SunionWifiService {
    companion object {
        val SERVICE_UUID = "fc3d8cf8-4ddc-7ade-1dd9-2497851131d7"
        val CMD_CHARACTERISTOC_UUID = "de915dce-3539-61ea-ade7-d44a2237601f"

    }

    fun collectWifiList(lockInfo: LockInfo): Flow<WifiListResult>

    suspend fun scanWifi(lockInfo: LockInfo)

    fun collectConnectToWifiState(lockInfo: LockInfo): Flow<WifiConnectState>

    suspend fun connectLockToWifi(ssid: String, password: String, lockInfo: LockInfo): Boolean
}


sealed class WifiListResult {
    data class Wifi(val ssid: String, val needPassword: Boolean) : WifiListResult()
    object End : WifiListResult()
}