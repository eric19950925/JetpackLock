package com.sunion.jetpacklock.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunion.jetpacklock.R

@Composable
fun InputTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    protect: Boolean = false,
    error: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    titleTextStyle: TextStyle = TextStyle(
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    subtitle: String? = null,
    subtitleTextStyle: TextStyle = TextStyle(
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = titleTextStyle)
            if (error.isNotEmpty())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = "warning"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = error,
                        style = TextStyle(
                            color = colorResource(id = R.color.redE60a17),
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    )
                }
        }

        if (subtitle?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))
            Text(text = subtitle, style = subtitleTextStyle)
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_14)))
        IKeyTextField(
            value = value,
            onValueChange = onValueChange,
            hasError = error.isNotEmpty(),
            hint = hint,
            protected = protect,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.testTag("input")
        )
    }
}

@Preview(showBackground = true, name = "Empty value")
@Composable
private fun Preview() {
//    FuhsingSmartLockV2AndroidTheme {
        InputTextField(
            title = stringResource(id = R.string.account_email),
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            hint = "example@mail.com"
        )
//    }
}

@Preview(showBackground = true, name = "Value")
@Composable
private fun Preview2() {
//    FuhsingSmartLockV2AndroidTheme {
        InputTextField(
            title = stringResource(id = R.string.account_email),
            value = "example@mail.com",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            hint = "example@mail.com",
            subtitle = stringResource(id = R.string.account_please_enter_your_email)
        )
//    }
}

@Preview(showBackground = true, name = "Value protected")
@Composable
private fun Preview3() {
//    FuhsingSmartLockV2AndroidTheme {
        InputTextField(
            title = stringResource(id = R.string.account_password),
            value = "1234567890",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            hint = "1234567890",
            protect = true
        )
//    }
}

@Preview(showBackground = true, name = "error")
@Composable
private fun Preview4() {
//    FuhsingSmartLockV2AndroidTheme {
        InputTextField(
            title = stringResource(id = R.string.account_email),
            value = "example@mail.com",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            hint = "example@mail.com",
            error = "User does not exist."
        )
//    }
}