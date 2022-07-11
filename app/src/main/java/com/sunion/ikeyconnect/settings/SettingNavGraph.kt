package com.sunion.ikeyconnect.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.settings.event_log.EventLogScreen
import com.sunion.ikeyconnect.settings.event_log.EventLogViewModel

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.settingGraph(
    navController: NavController,
    route: String
) {
    navigation(startDestination = "${SettingRoute.Setting.route}/{macAddress}/{isConnected}", route = route) {
        composable("${SettingRoute.Setting.route}/{macAddress}/{isConnected}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val isConnected =
                backStackEntry.arguments?.getString("isConnected")?.toBoolean() ?: false
            val viewModel = hiltViewModel<SettingsViewModel>()
            viewModel.macAddressOrThingName ?: viewModel.init(macAddress, isConnected)
            SettingsScreen(
                viewModel = viewModel,
                navController = navController,
            )
        }
        composable("${SettingRoute.EventLog.route}/{thingName}/{deviceName}") { backStackEntry ->
            val thingName = backStackEntry.arguments?.getString("thingName") ?: ""
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: ""
            val viewModel = hiltViewModel<EventLogViewModel>()
            viewModel.thingName ?: viewModel.init(thingName, deviceName)
            EventLogScreen(viewModel = viewModel, navController = navController)
        }
    }
}


sealed class SettingRoute(val route: String) {
    object Setting : SettingRoute("Setting")
    object EventLog : SettingRoute("EventLog")
}