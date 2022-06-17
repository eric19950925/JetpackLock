package com.sunion.ikeyconnect.add_lock.connect_to_wifi

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog

@Composable
fun DisconnectionDialog(onConfirmButtonClick: () -> Unit) {
    IkeyAlertDialog(
        onDismissRequest = {},
        onConfirmButtonClick = onConfirmButtonClick,
        title = stringResource(id = R.string.connect_to_wifi_disconnection),
        text = stringResource(id = R.string.connect_to_wifi_lose_bluetooth_connection)
    )
}

@Preview
@Composable
private fun PreviewDisconnectionDialog() {
    DisconnectionDialog(onConfirmButtonClick = {})
}