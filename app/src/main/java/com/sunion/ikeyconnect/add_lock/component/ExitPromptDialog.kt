package com.sunion.ikeyconnect.add_lock.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog

@Composable
fun ExitPromptDialog(
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit
) {
    IkeyAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmButtonClick = onConfirmButtonClick,
        title = stringResource(id = R.string.location_setup_cancel_title),
        text = stringResource(id = R.string.location_setup_cancel_message),
        onDismissButtonClick = onDismissButtonClick,
        dismissButtonText = stringResource(id = R.string.global_cancel),
        confirmButtonText = stringResource(id = R.string.global_ok_uppercase)
    )
}