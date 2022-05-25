package com.sunion.ikeyconnect

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunion.ikeyconnect.account.AccountActivity
import com.sunion.ikeyconnect.account.AccountNavigation
import com.sunion.ikeyconnect.account.LoginViewModel
import com.sunion.ikeyconnect.home.HomeNavHost
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import com.sunion.ikeyconnect.R

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
        const val AES_KEY = ""
        const val AES_KEY2 = ""
        const val AES_KEY3 = ""
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
