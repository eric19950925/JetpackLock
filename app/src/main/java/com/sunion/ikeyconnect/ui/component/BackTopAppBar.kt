package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18

@Composable
fun BackTopAppBar(onNaviUpClick: () -> Unit, modifier: Modifier = Modifier, title: String? = null) {
    TopAppBar(
        navigationIcon = {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable(onClick = onNaviUpClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "back",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            if (title != null)
                Text(text = title, style = MaterialTheme.typography.colorPrimaryBoldSize18)
        },
        backgroundColor = Color.White,
        elevation = 0.dp,
        modifier = modifier
    )
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        BackTopAppBar({})
    }
}

@Preview
@Composable
private fun PreviewWithTitle() {
    FuhsingSmartLockV2AndroidTheme {
        BackTopAppBar({}, title = stringResource(id = R.string.member_account))
    }
}