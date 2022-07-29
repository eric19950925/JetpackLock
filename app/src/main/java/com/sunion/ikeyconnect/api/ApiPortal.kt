package com.sunion.ikeyconnect.api

sealed class APIObject(val route: String) {
    object DeviceList: APIObject("device-list")
    object GetUserSync: APIObject("user-sync/get")
    object UpdateUserSync: APIObject("user-sync/update")
    object DeviceProvision: APIObject("device-provision")
}