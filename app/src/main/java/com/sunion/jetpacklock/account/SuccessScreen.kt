package com.sunion.jetpacklock.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunion.jetpacklock.ui.component.PrimaryButton
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.ui.theme.colorPrimaryBoldSize18
import com.sunion.jetpacklock.R

@Composable
fun SuccessScreen(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    text: String,
    buttonText: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 56.dp)
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_85)))
        Text(
            text = title,
            style = MaterialTheme.typography.colorPrimaryBoldSize18,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_15)))
        Text(
            text = text,
            style = TextStyle(
                MaterialTheme.colors.primary,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_56)))
        Image(
            painter = painterResource(id = R.drawable.ic_check_circle),
            contentDescription = "success",
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.space_72))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.space_56))
                .align(Alignment.CenterHorizontally)
        )
        PrimaryButton(
            text = buttonText,
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        SuccessScreen(
            onClick = {},
            title = stringResource(id = R.string.account_welcome),
            text = stringResource(id = R.string.account_your_account_has_been_successfully_created),
            buttonText = stringResource(id = R.string.account_lets_start)
        )
    }
}