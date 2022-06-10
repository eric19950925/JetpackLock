package com.sunion.ikeyconnect.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jakewharton.processphoenix.ProcessPhoenix
import com.sunion.ikeyconnect.account_management.memberGraph
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.addLockGraph

@Composable
fun HomeNavHost(onLogoutClick: () -> Unit) {
    val navController = rememberNavController()
    val mContext = LocalContext.current
    NavHost(navController = navController, startDestination = HomeRoute.Home.route) {
        composable(HomeRoute.Home.route) {
            val viewModel = hiltViewModel<HomeViewModel>()
            HomeScreen(
                onAddLockClick = {
                    navController.navigate(HomeRoute.AddLock.route)
                },
                onPersonClick = {
                    navController.navigate(HomeRoute.MemberManagement.route)
                },
                showGuile = viewModel.showGuide.value,
                onShowGuideClick = {viewModel.setGuideHasBeenSeen()}
            )
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
//            onAddLockFinish = viewModel::boltOrientation
        )
    }
}

sealed class HomeRoute(val route: String) {
    object Home : HomeRoute("MemberHome")
    object MemberManagement : HomeRoute("MemberManagement")
    object AddLock : HomeRoute("AddLock")
}