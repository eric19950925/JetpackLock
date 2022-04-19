package com.sunion.jetpacklock

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunion.jetpacklock.account.AccountNavigation
import com.sunion.jetpacklock.account.LoginViewModel
import com.sunion.jetpacklock.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavigationComponent(navController)
        }
    }
    companion object {
        const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"
    }
}

@Composable
fun NavigationComponent(navController: NavHostController) {
    val vm = viewModel<LoginViewModel>()
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            AccountNavigation(
                onLoginSuccess= {
                    vm.setAttachPolicy()
                    navController.navigate("home")
                                },
                vm
            )
        }
        composable("home") {
            HomeScreen(
                onLogOutClick = {
                    vm.logOut()
                    navController.navigate("login")
                },
                onGetTimeClick = { vm.getTime() }
            )
        }
    }
}