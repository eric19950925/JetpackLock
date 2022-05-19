package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R

@Composable
fun LoadingScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.DarkGray)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoadingScreenDialog() {
    Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LoadingScreen()
    }
}

@Preview
@Composable
private fun PreviewLoadingScreen() {
    FuhsingSmartLockV2AndroidTheme {
        LoadingScreen()
    }
}