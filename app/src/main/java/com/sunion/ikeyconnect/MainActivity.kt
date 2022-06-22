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
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.sunion.ikeyconnect.account.AccountActivity
import com.sunion.ikeyconnect.account.AccountNavigation
import com.sunion.ikeyconnect.account.LoginViewModel
import com.sunion.ikeyconnect.home.HomeNavHost
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import com.sunion.ikeyconnect.home.HomeViewModel
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var statefulConnection: StatefulConnection

    @Inject
    lateinit var mqttManager: AWSIotMqttManager

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

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttManager.disconnect()
            Timber.d("mqttDisconnect success.")
        }catch (e: Exception){
            Timber.d( "mqttDisconnect error.", e)
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
    val loginViewModel = viewModel<LoginViewModel>()
    val homeViewModel = viewModel<HomeViewModel>()
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                loginViewModel,
                toHome = {
                    navController.navigate("home")
                    loginViewModel.setCredentialsProvider()
                },
                toLogin = {
                    navController.navigate("login")
                },
                logOut = {
                    loginViewModel.logOut()
                }
            )
        }
        composable("login") {
            AccountNavigation(
                onLoginSuccess= {
                    loginViewModel.setAttachPolicy()
                    onLoginSuccess.invoke()
                                }
            )
        }
        composable("home") {
            HomeNavHost(
                homeViewModel,
                onLogoutClick = onLogoutClick
            )
        }
    }
}
