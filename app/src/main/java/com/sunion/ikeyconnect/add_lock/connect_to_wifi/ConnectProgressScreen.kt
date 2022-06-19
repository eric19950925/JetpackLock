package com.sunion.ikeyconnect.add_lock.connect_to_wifi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R

@Composable
fun ConnectProgressScreen(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_60)))
        IKeyDivider()
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_99)))
        CircularProgressIndicator(modifier = Modifier.size(dimensionResource(id = R.dimen.space_150)))
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_30)))
        Text(
            text = message,
            style = TextStyle(
                color = Color(0xFF7496AF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Preview
@Composable
private fun PreviewConnectProgressScreen() {
    FuhsingSmartLockV2AndroidTheme {
        ConnectProgressScreen(stringResource(id = R.string.connect_to_wifi_loading))
    }
}