package com.sunion.ikeyconnect.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jakewharton.processphoenix.ProcessPhoenix
import com.sunion.ikeyconnect.account_management.memberGraph
import com.sunion.ikeyconnect.add_lock.addLockGraph

@Composable
fun HomeNavHost(viewModel: HomeViewModel, onLogoutClick: () -> Unit) {
    val navController = rememberNavController()
    val mContext = LocalContext.current
    NavHost(navController = navController, startDestination = HomeRoute.Home.route) {
        composable(HomeRoute.Home.route) {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
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
            onAddLockFinish = viewModel::boltOrientation
        )
        homeTestGraph(
            navController = navController,
            route = HomeRoute.HomeTest.route
        )

    }
}

sealed class HomeRoute(val route: String) {
    object Home : HomeRoute("MemberHome")
    object MemberManagement : HomeRoute("MemberManagement")
    object AddLock : HomeRoute("AddLock")
    object HomeTest : HomeRoute("HomeTest")
    object Settings : HomeRoute("Settings")
}