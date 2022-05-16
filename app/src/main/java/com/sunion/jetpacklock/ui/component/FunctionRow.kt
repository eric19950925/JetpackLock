package com.sunion.jetpacklock.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.ui.theme.colorPrimaryMediumSize16

@Composable
fun FunctionRow(
    onClick: () -> Unit,
    text: String,
    textStyle: TextStyle = MaterialTheme.typography.colorPrimaryMediumSize16
) {
    Text(
        text = text,
        style = textStyle,
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.light_primary))
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(id = R.dimen.space_28),
                vertical = dimensionResource(id = R.dimen.space_16)
            )
    )
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        FunctionRow(onClick = { }, text = "ABC")
    }
}