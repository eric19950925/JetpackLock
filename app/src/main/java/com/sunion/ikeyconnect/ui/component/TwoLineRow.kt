package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryMediumSize16
import com.sunion.ikeyconnect.ui.theme.colorPrimaryRegularSize12
import com.sunion.ikeyconnect.R

@Composable
fun TwoLineRow(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.space_26),
                vertical = dimensionResource(id = R.dimen.space_15)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.colorPrimaryRegularSize12
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_7)))
            Text(
                text = text,
                style = MaterialTheme.typography.colorPrimaryMediumSize16
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showArrow)
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "next",
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.space_15))
                    .rotate(180f)
            )
    }
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        TwoLineRow(title = "title", text = "texttexttexttexttexttexttexttext")
    }
}