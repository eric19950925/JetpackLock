package com.sunion.ikeyconnect.add_lock.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog

@Composable
fun EnableBluetoothDialog(
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit,
) {
    IkeyAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmButtonClick = onConfirmButtonClick,
        title = stringResource(id = R.string.dialog_rationale_bluetooth_title),
        text = stringResource(id = R.string.rationale_bluetooth_android),
        confirmButtonText = stringResource(id = R.string.global_enable),
        onDismissButtonClick = onDismissButtonClick,
        dismissButtonText = stringResource(id = R.string.global_cancel),
    )
}

@Preview
@Composable
private fun PreviewEnableBluetoothDialog() {
    EnableBluetoothDialog(
        onDismissRequest = {},
        onConfirmButtonClick = {},
        onDismissButtonClick = {}
    )
}