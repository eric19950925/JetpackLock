package com.sunion.ikeyconnect.settings.wifi_setting

sealed class WiFiSettingRoute(val route: String) {
    object Pairing : WiFiSettingRoute("wifi_setting_pairing")
    object WifiList : WiFiSettingRoute("wifi_setting_wifi_list")
    object ConnectWifi : WiFiSettingRoute("wifi_setting_connect_wifi")
}