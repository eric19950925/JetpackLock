package com.sunion.jetpacklock

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunion.jetpacklock.account.AccountActivity
import com.sunion.jetpacklock.account.AccountNavigation
import com.sunion.jetpacklock.account.LoginViewModel
import com.sunion.jetpacklock.account_management.AccountScreen
import com.sunion.jetpacklock.account_management.MemberManagementRoute
import com.sunion.jetpacklock.account_management.MemberManagementViewModel
import com.sunion.jetpacklock.home.HomeNavHost
import com.sunion.jetpacklock.home.HomeRoute
import com.sunion.jetpacklock.home.HomeScreen
import com.sunion.jetpacklock.home.HomeViewModel
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            FuhsingSmartLockV2AndroidTheme {
                NavigationComponent(
                    navController,
                    onLogoutClick = this::goLogin,
                    onLoginSuccess = this::goHome
                )
            }
        }
    }
    companion object {
        const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"
    }
    private fun goLogin() {
        startActivity(Intent(this, AccountActivity::class.java))
        finish()
    }
    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun NavigationComponent(navController: NavHostController, onLogoutClick: () -> Unit, onLoginSuccess: () -> Unit) {
    val vm = viewModel<LoginViewModel>()
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                vm,
                toHome = {
                    navController.navigate("home")
                },
                toLogin = {
                    navController.navigate("login")
                },
                logOut = {
                    vm.logOut()
                }
            )
        }
        composable("login") {
            AccountNavigation(
                onLoginSuccess= {
                    vm.setAttachPolicy()
                    onLoginSuccess.invoke()
                                }
            )
        }
        composable("home") {
            HomeNavHost(
                onLogoutClick = onLogoutClick
            )
        }
    }
}
