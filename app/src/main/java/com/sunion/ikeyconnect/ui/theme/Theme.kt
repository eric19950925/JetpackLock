package com.sunion.ikeyconnect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.google.accompanist.appcompattheme.createAppCompatTheme
import com.sunion.ikeyconnect.R


@Composable
fun FuhsingSmartLockV2AndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var (colors, type) = context.createAppCompatTheme()

    colors = colors?.copy(primaryVariant = colorResource(id = R.color.primaryVariant))

    MaterialTheme(
        colors = colors ?: MaterialTheme.colors,
        typography = type ?: MaterialTheme.typography
    ) {
        content()
    }
}