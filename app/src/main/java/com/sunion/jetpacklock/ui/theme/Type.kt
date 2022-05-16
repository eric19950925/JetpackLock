package com.sunion.jetpacklock.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
    /* Other default text styles to override
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
    */
)

val Typography.colorPrimaryBoldSize18: TextStyle
    @Composable
    get() = TextStyle(
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

val Typography.colorPrimaryMediumSize18: TextStyle
    @Composable
    get() = TextStyle(
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    )

val Typography.colorPrimaryRegularSize12: TextStyle
    @Composable
    get() = TextStyle(
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )

val Typography.colorPrimaryMediumSize16: TextStyle
    @Composable
    get() = TextStyle(
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )