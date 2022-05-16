package com.sunion.jetpacklock.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunion.jetpacklock.account_management.memberGraph

@Composable
fun HomeNavHost(onLogoutClick: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute.Home.route) {
        composable(HomeRoute.Home.route) {
            val viewModel = hiltViewModel<HomeViewModel>()
            HomeScreen(
                onAddLockClick = {

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
            onSignOutSuccess = onLogoutClick,
            route = HomeRoute.MemberManagement.route
        )
    }
}