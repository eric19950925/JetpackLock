package com.sunion.jetpacklock.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.flow.collect
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun PasswordTooltip(offset: Offset) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = offset.x.toInt(),
            y = offset.y.toInt() + 20
        )
    ) {
        PasswordTooltipContent()
    }
}

@Composable
private fun PasswordTooltipContent() {
    Row(
        modifier = Modifier
            .width(dimensionResource(id = R.dimen.space_286))
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(
                BorderStroke(1.dp, colorResource(id = R.color.primaryVariant)),
                RoundedCornerShape(4.dp)
            )
            .padding(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_warning),
            contentDescription = "warning",
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = stringResource(id = R.string.account_password_tips),
            style = TextStyle(
                color = colorResource(id = R.color.redE60a17),
                fontSize = 10.sp
            )
        )
    }
}

@Preview
@Composable
private fun PreviewPasswordTooltipContent() {
    FuhsingSmartLockV2AndroidTheme {
        PasswordTooltipContent()
    }
}