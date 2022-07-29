package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.theme.colorPrimaryRegularSize12

@Composable
fun IkeyAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String = stringResource(id = R.string.global_ok_uppercase),
    dismissButtonText: String? = null,
    onDismissButtonClick: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmButtonClick) {
                Text(text = confirmButtonText)
            }
        },
        title = { Text(text = title) },
        text = { Text(text = text) },
        backgroundColor = Color.White,
        dismissButton = if (dismissButtonText == null) null
        else {
            {
                TextButton(onClick = { onDismissButtonClick?.invoke() }) {
                    Text(text = dismissButtonText, color = Color.Red)
                }
            }
        }
    )
}

@Composable
fun IkeyAlertEditDialog(
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    title: String,
    text: String,
    textFieldSubTitle: String,
    textFieldValue: String,
    onTextFieldChange: (String) -> Unit,
    confirmButtonText: String = stringResource(id = R.string.global_ok_uppercase),
    dismissButtonText: String? = null,
    onDismissButtonClick: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmButtonClick) {
                Text(text = confirmButtonText)
            }
        },
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = text)
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_13)))
                Text(
                    text = textFieldSubTitle,
                    style = MaterialTheme.typography.colorPrimaryRegularSize12
                )
                IKeyTextField(
                    value = textFieldValue,
                    onValueChange = onTextFieldChange,
                    protected = true
                )
            }

               },
        backgroundColor = Color.White,
        dismissButton = if (dismissButtonText == null) null
        else {
            {
                TextButton(onClick = { onDismissButtonClick?.invoke() }) {
                    Text(text = dismissButtonText, color = Color.Red)
                }
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        IkeyAlertDialog(
            onDismissRequest = { },
            onConfirmButtonClick = { },
            title = "Title",
            text = "TextTextTextTextTextText"
        )
    }
}

@Preview
@Composable
private fun Preview2() {
    FuhsingSmartLockV2AndroidTheme {
        IkeyAlertDialog(
            onDismissRequest = { },
            onConfirmButtonClick = { },
            title = "Title",
            text = "TextTextTextTextTextText",
            dismissButtonText = stringResource(id = R.string.global_cancel)
        )
    }
}