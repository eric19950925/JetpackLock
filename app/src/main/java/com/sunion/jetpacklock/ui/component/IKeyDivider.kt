package com.sunion.jetpacklock.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun IKeyDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier,
        color = colorResource(id = R.color.disable_grey)
    )
}

@Preview
@Composable
private fun PreviewIKeyDivider() {
    FuhsingSmartLockV2AndroidTheme {
        IKeyDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )
    }
}