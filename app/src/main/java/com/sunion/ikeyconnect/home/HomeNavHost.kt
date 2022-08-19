package com.sunion.ikeyconnect.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jakewharton.processphoenix.ProcessPhoenix
import com.sunion.ikeyconnect.access_code.userCodeRouter
import com.sunion.ikeyconnect.account_management.memberGraph
import com.sunion.ikeyconnect.add_lock.addLockGraph
import com.sunion.ikeyconnect.auto_unlock.SetLocationScreen
import com.sunion.ikeyconnect.auto_unlock.autoUnLockGraph
import com.sunion.ikeyconnect.settings.settingGraph
import com.sunion.ikeyconnect.users.UsersScreen
import com.sunion.ikeyconnect.users.UsersViewModel
import com.sunion.ikeyconnect.users.usersRouter

@Composable
fun HomeNavHost(viewModel: HomeViewModel, onLogoutClick: () -> Unit) {
    val navController = rememberNavController()
    val mContext = LocalContext.current
    NavHost(navController = navController, startDestination = HomeRoute.Home.route) {
        composable(HomeRoute.Home.route) {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
        usersRouter(
            navController = navController,
            route = "${HomeRoute.Users.route}/{macAddress}/{deviceType}",
        )
        userCodeRouter(
            navController = navController,
            route = "${HomeRoute.UserCode.route}/{macAddress}/{deviceType}",
        )
        autoUnLockGraph(
            navController = navController,
            route = "${HomeRoute.AutoUnLock.route}/{macAddress}/{deviceType}",
        )
        memberGraph(
            navController = navController,
            onSignOutSuccess = {
//                onLogoutClick.invoke()
                ProcessPhoenix.triggerRebirth(mContext)
            }
            ,route = HomeRoute.MemberManagement.route
        )
        addLockGraph(
            navController = navController,
            route = HomeRoute.AddLock.route,
            onAddLockFinish = { macAddress ->
                viewModel.boltOrientation(macAddress)
                viewModel.turnToTheLastPage()
//                viewModel.getDeviceList()
            }
        )
        settingGraph(
            navController = navController,
            route = "${HomeRoute.Settings.route}/{macAddress}/{isConnected}/{battery}/{deviceType}",
        )
//        composable("${HomeRoute.Settings.route}/{macAddress}/{isConnected}") { backStackEntry ->
//            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
//            val isConnected =
//                backStackEntry.arguments?.getString("isConnected")?.toBoolean() ?: false
//            val settingsViewModel = hiltViewModel<SettingsViewModel>()
//            settingsViewModel.macAddress ?: settingsViewModel.init(macAddress, isConnected)
//            SettingsScreen(viewModel = settingsViewModel, navController = navController)
//        }
    }
}

sealed class HomeRoute(val route: String) {
    object Home : HomeRoute("MemberHome")
    object MemberManagement : HomeRoute("MemberManagement")
    object AddLock : HomeRoute("AddLock")
    object HomeTest : HomeRoute("HomeTest")
    object Settings : HomeRoute("Settings")
    object AutoUnLock : HomeRoute("AutoUnLock")
    object Users : HomeRoute("Users")
    object UserCode : HomeRoute("UserCode")
}