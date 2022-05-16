package com.sunion.jetpacklock.account_management

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.sunion.jetpacklock.account.AccountNavigation
import com.sunion.jetpacklock.account.LoginScreen
import com.sunion.jetpacklock.account.LoginViewModel
import com.sunion.jetpacklock.account_management.my_profile.ChangeNameScreen
import com.sunion.jetpacklock.account_management.my_profile.MyProfileViewModel
import com.sunion.jetpacklock.account_management.security.ChangePasswordScreen
import com.sunion.jetpacklock.account_management.security.SecurityScreen
import com.sunion.jetpacklock.account_management.security.SecurityViewModel

fun NavGraphBuilder.memberGraph(
    navController: NavController,
    onSignOutSuccess: () -> Unit,
    route: String
) {
    navigation(startDestination = MemberManagementRoute.Home.route, route = route) {
        composable(MemberManagementRoute.Home.route) {
            val viewModel = hiltViewModel<MemberManagementViewModel>()
            viewModel.loadData()
            AccountScreen(
                viewModel = viewModel,
                navController = navController,
                onNaviUpClick = { navController.popBackStack() },
                onSignOutSuccess = onSignOutSuccess
            )
        }
        composable(MemberManagementRoute.MyProfile.route) {
            val viewModel = hiltViewModel<MyProfileViewModel>()
            viewModel.loadData()
            MyProfileScreen(viewModel = viewModel, navController = navController)
        }
        composable(MemberManagementRoute.ChangeName.route) {
            val viewModel = hiltViewModel<MyProfileViewModel>()
            viewModel.loadData()
            ChangeNameScreen(viewModel = viewModel, navController = navController)
        }
        composable(MemberManagementRoute.Security.route) {
            SecurityScreen(viewModel = hiltViewModel(), navController = navController)
        }
        composable(MemberManagementRoute.ChangePassword.route) {
            val parentEntry =
                remember { navController.getBackStackEntry(MemberManagementRoute.Security.route) }
            val viewModel = hiltViewModel<SecurityViewModel>(parentEntry)
            ChangePasswordScreen(viewModel = viewModel, navController = navController)
        }
        composable(MemberManagementRoute.LogIn.route) {
            val vm = hiltViewModel<LoginViewModel>()
            AccountNavigation(
                onLoginSuccess= {
                    vm.setAttachPolicy()
                    navController.navigate("home")
                }
            )
        }
    }
}


sealed class MemberManagementRoute(val route: String) {
    object Home : MemberManagementRoute("Home")
    object LogIn : MemberManagementRoute("LogIn")
    object MyProfile : MemberManagementRoute("MyProfile")
    object ChangeName : MemberManagementRoute("ChangeName")
    object Security : MemberManagementRoute("Security")
    object ChangePassword : MemberManagementRoute("ChangePassword")
}