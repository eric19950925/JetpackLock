package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R

@Composable
fun LoadingScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colorResource(id = R.color.primary))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoadingScreenDialog(msg: String) {
    Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LoadingScreen()
        Text(
            text = msg,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 600.dp),
            textAlign = TextAlign.Center,
            style = TextStyle(color = colorResource(id = R.color.primary)),
        )
    }
}

@Preview
@Composable
private fun PreviewLoadingScreen() {
    FuhsingSmartLockV2AndroidTheme {
        LoadingScreenDialog("loading")
    }
}