package com.sunion.ikeyconnect.add_lock

sealed class AddLockRoute(val route: String) {
    object ScanPermission : AddLockRoute("add_lock_scan_permission")
    object Scan : AddLockRoute("add_lock_scan")
    object Installation : AddLockRoute("add_lock_installation")
    object InstallationInstructions : AddLockRoute("add_lock_installation_instructions")
    object Pairing : AddLockRoute("add_lock_pairing")
    object WifiList : AddLockRoute("add_lock_wifi_list")
    object ConnectWifi : AddLockRoute("add_lock_connect_wifi")
    object AdminCode : AddLockRoute("add_lock_admin_code")
    object RequestLocation : AddLockRoute("add_lock_request_location")
    object SetLocation : AddLockRoute("add_lock_set_location")
    object LockOverview : AddLockRoute("add_lock_lock_overview")
}
