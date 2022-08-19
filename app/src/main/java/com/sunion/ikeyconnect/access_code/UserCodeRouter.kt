package com.sunion.ikeyconnect.access_code

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.sunion.ikeyconnect.mapDeviceType
import com.sunion.ikeyconnect.users.AccessCodeViewModel

fun NavGraphBuilder.userCodeRouter(
    navController: NavController,
    route: String
) {
    navigation(startDestination = "${UserCodeRoute.UserCodePage.route}/{macAddress}/{deviceType}", route = route) {
        composable("${UserCodeRoute.UserCodePage.route}/{macAddress}/{deviceType}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val deviceTypeString = backStackEntry.arguments?.getString("deviceType") ?: ""
            val deviceType = mapDeviceType(deviceTypeString)
            val isConnected =
                backStackEntry.arguments?.getString("isConnected")?.toBoolean() ?: false
            val viewModel = hiltViewModel<AccessCodeViewModel>()
            viewModel.deviceIdentity ?: viewModel.init(macAddress, deviceType)
            AccessCodeScreen(
                viewModel = viewModel,
                navController = navController,
            )
        }
    }
}
sealed class UserCodeRoute(val route: String) {
    object UserCodePage : UserCodeRoute("UserCodePage")

}
