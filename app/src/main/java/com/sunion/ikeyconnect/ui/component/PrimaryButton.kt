package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.sunion.ikeyconnect.R

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = dimensionResource(id = R.dimen.space_41),
    width: Dp = dimensionResource(id = R.dimen.space_167),
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier.size(width = width, height = height)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
    }
}

@Preview
@Composable
private fun Preview() {
//    FuhsingSmartLockV2AndroidTheme {
        PrimaryButton(onClick = {}, text = stringResource(id = R.string.account_log_in))
//    }
}