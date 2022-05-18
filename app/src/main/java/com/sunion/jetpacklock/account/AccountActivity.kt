package com.sunion.jetpacklock.account

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sunion.jetpacklock.MainActivity
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FuhsingSmartLockV2AndroidTheme {
                AccountNavigation(onLoginSuccess = this::goHome)
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}