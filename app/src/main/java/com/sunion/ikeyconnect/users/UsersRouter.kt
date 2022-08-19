package com.sunion.ikeyconnect.users

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.sunion.ikeyconnect.mapDeviceType

fun NavGraphBuilder.usersRouter(
    navController: NavController,
    route: String
) {
    navigation(startDestination = "${UsersRoute.UsersPage.route}/{macAddress}/{deviceType}", route = route) {
        composable("${UsersRoute.UsersPage.route}/{macAddress}/{deviceType}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val deviceTypeString = backStackEntry.arguments?.getString("deviceType") ?: ""
            val deviceType = mapDeviceType(deviceTypeString)
            val isConnected =
                backStackEntry.arguments?.getString("isConnected")?.toBoolean() ?: false
            val viewModel = hiltViewModel<UsersViewModel>()
            viewModel.deviceIdentity ?: viewModel.init(macAddress, deviceType)
            UsersScreen(
                viewModel = viewModel,
                navController = navController,
            )
        }
    }
}
sealed class UsersRoute(val route: String) {
    object UsersPage : UsersRoute("UsersPage")

}
