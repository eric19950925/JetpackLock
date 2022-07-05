package com.sunion.ikeyconnect.add_lock.component

import com.sunion.ikeyconnect.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@Composable
fun CameraDeniedContent(
    shouldShowRationale: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        val message = if (shouldShowRationale) {
            stringResource(R.string.rationale_camera)
        } else {
            stringResource(R.string.rationale_camera)
        }

        Text(message)
        Button(onClick = onClick) {
            Text(stringResource(R.string.global_ok_uppercase))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    CameraDeniedContent(shouldShowRationale = true, onClick = { })
}