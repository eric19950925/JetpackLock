package com.sunion.ikeyconnect.domain.Interface

import com.polidea.rxandroidble2.RxBleConnection

interface WifiSettingDomain {
    fun settingSSID()
    fun settingPassword()
    fun settingConnection()
    suspend fun searchWifi(input1: String, input2: RxBleConnection)
}