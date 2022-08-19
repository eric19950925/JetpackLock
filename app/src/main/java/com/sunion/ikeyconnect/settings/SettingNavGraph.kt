package com.sunion.ikeyconnect.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.sunion.ikeyconnect.mapDeviceType
import com.sunion.ikeyconnect.settings.change_admin_code.ChangeAdminCodeScreen
import com.sunion.ikeyconnect.settings.change_admin_code.ChangeAdminCodeViewModel
import com.sunion.ikeyconnect.settings.event_log.EventLogScreen
import com.sunion.ikeyconnect.settings.event_log.EventLogViewModel
import com.sunion.ikeyconnect.settings.wifi_setting.WiFiSettingPairingScreen
import com.sunion.ikeyconnect.settings.wifi_setting.WiFiSettingViewModel

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.settingGraph(
    navController: NavController,
    route: String
) {
    navigation(startDestination = "${SettingRoute.Setting.route}/{macAddress}/{isConnected}/{battery}/{deviceType}", route = route) {
        composable("${SettingRoute.Setting.route}/{macAddress}/{isConnected}/{battery}/{deviceType}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val battery = backStackEntry.arguments?.getString("battery") ?: "0"
            val deviceTypeString = backStackEntry.arguments?.getString("deviceType") ?: ""
            val deviceType = mapDeviceType(deviceTypeString)
            val isConnected =
                backStackEntry.arguments?.getString("isConnected")?.toBoolean() ?: false
            val viewModel = hiltViewModel<SettingsViewModel>()
            viewModel.deviceIdentity ?: viewModel.init(macAddress, isConnected, battery, deviceType)
            SettingsScreen(
                viewModel = viewModel,
                navController = navController,
            )
        }
        composable("${SettingRoute.EventLog.route}/{thingName}/{deviceName}/{deviceType}") { backStackEntry ->
            val thingName = backStackEntry.arguments?.getString("thingName") ?: ""
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: ""
            val deviceTypeString = backStackEntry.arguments?.getString("deviceType") ?: ""
            val deviceType = mapDeviceType(deviceTypeString)
            val viewModel = hiltViewModel<EventLogViewModel>()
            viewModel.thingName ?: viewModel.init(thingName, deviceName, deviceType)
            EventLogScreen(viewModel = viewModel, navController = navController)
        }
        composable("${SettingRoute.ChangeAdminCode.route}/{thingName}/{deviceType}") { backStackEntry ->
            val thingName = backStackEntry.arguments?.getString("thingName") ?: ""
            val deviceTypeString = backStackEntry.arguments?.getString("deviceType") ?: ""
            val deviceType = mapDeviceType(deviceTypeString)
            val viewModel = hiltViewModel<ChangeAdminCodeViewModel>()
            viewModel.deviceIdentity ?: viewModel.init(thingName, deviceType)
            ChangeAdminCodeScreen(viewModel = viewModel, navController = navController)
        }
        composable("${SettingRoute.WiFiSetting.route}/{thingName}/{MACAddress}/{BroadcastName}/{ConnectionKey}/{ShareToken}") { backStackEntry ->
            val thingName = backStackEntry.arguments?.getString("thingName") ?: ""
            val MACAddress = backStackEntry.arguments?.getString("MACAddress") ?: ""
            val BroadcastName = backStackEntry.arguments?.getString("BroadcastName") ?: ""
            val ConnectionKey = backStackEntry.arguments?.getString("ConnectionKey") ?: ""
            val ShareToken = backStackEntry.arguments?.getString("ShareToken") ?: ""
            val viewModel = hiltViewModel<WiFiSettingViewModel>()
            viewModel.macAddress ?: viewModel.init(thingName, MACAddress, BroadcastName, ConnectionKey, ShareToken)
            WiFiSettingPairingScreen(viewModel = viewModel, navController = navController)
        }
    }
}


sealed class SettingRoute(val route: String) {
    object Setting : SettingRoute("Setting")
    object EventLog : SettingRoute("EventLog")
    object ChangeAdminCode : SettingRoute("ChangeAdminCode")
    object WiFiSetting : SettingRoute("WiFiSetting")
}