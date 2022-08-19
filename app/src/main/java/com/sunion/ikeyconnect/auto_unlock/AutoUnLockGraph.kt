package com.sunion.ikeyconnect.auto_unlock

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.sunion.ikeyconnect.mapDeviceType

fun NavGraphBuilder.autoUnLockGraph(
    navController: NavController,
    route: String
) {
    navigation(startDestination = "${AutoUnLockRoute.AutoUnLockPage.route}/{macAddress}/{deviceType}", route = route) {
        composable("${AutoUnLockRoute.AutoUnLockPage.route}/{macAddress}/{deviceType}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val deviceTypeString = backStackEntry.arguments?.getString("deviceType") ?: ""
            val deviceType = mapDeviceType(deviceTypeString)
            val isConnected =
                backStackEntry.arguments?.getString("isConnected")?.toBoolean() ?: false
            val viewModel = hiltViewModel<AutoUnlockViewModel>()
            viewModel.deviceIdentity ?: viewModel.init(macAddress, deviceType)
            AutoUnlockScreen(
                viewModel = viewModel,
                navController = navController,
            )
        }
        composable("${AutoUnLockRoute.SettingLocation.route}/{thingName}/{deviceType}") { backStackEntry ->
            val thingName = backStackEntry.arguments?.getString("thingName") ?: ""
            val deviceTypeString = backStackEntry.arguments?.getString("deviceType") ?: ""
            val deviceType = mapDeviceType(deviceTypeString)
            val viewModel = hiltViewModel<AutoUnlockViewModel>()
            viewModel.deviceIdentity ?: viewModel.init(thingName, deviceType)
            SetLocationScreen(viewModel = viewModel, navController = navController)
        }
    }
}


sealed class AutoUnLockRoute(val route: String) {
    object AutoUnLockPage : AutoUnLockRoute("AutoUnLockPage")
    object SettingLocation : AutoUnLockRoute("SettingLocation")
}