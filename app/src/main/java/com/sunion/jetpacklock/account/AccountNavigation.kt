package com.sunion.jetpacklock.account

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun  AccountNavigation(onLoginSuccess: () -> Unit, loginViewModel: LoginViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            SimpleComposable(loginViewModel, navController, onLoginSuccess)
        }
    }
}