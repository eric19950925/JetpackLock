package com.sunion.ikeyconnect.add_lock.scan_qrcode

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.component.AddLockScreenScaffold
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanLockPermissionScreen(navController: NavController) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var isScanButtonClicked by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = cameraPermissionState.status.isGranted) {
        if (isScanButtonClicked && cameraPermissionState.status.isGranted)
            navController.navigate(AddLockRoute.Scan.route)
    }

    ScanLockPermissionScreen(
        onNaviUpClick = navController::popBackStack,
        onScanClick = {
            isScanButtonClicked = true
            when {
                cameraPermissionState.status.isGranted ->
                    navController.navigate(AddLockRoute.Scan.route)
                cameraPermissionState.status.shouldShowRationale ->
                    Unit
                else ->
                    cameraPermissionState.launchPermissionRequest()
            }
        }
    )

    if (cameraPermissionState.status.shouldShowRationale) {
        IkeyAlertDialog(
            onDismissRequest = { },
            onConfirmButtonClick = {
                cameraPermissionState.launchPermissionRequest()
            },
            title = "",
            text = stringResource(R.string.rationale_camera)
        )
    }
}

@Composable
fun ScanLockPermissionScreen(
    onNaviUpClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AddLockScreenScaffold(
        onNaviUpClick = onNaviUpClick,
        title = stringResource(id = R.string.toolbar_title_add_lock),
        step = 1,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(0.5f))
        Image(
            painter = painterResource(id = R.drawable.ble_scan),
            contentDescription = null,
            modifier = Modifier.height(dimensionResource(id = R.dimen.space_287))
        )
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = stringResource(id = R.string.scan_instruction),
            style = MaterialTheme.typography.colorPrimaryBoldSize18.copy(textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.space_55))
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(text = stringResource(id = R.string.global_scan), onClick = onScanClick)
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
    }
}

@Preview
@Composable
private fun PreviewDefault() {
    FuhsingSmartLockV2AndroidTheme {
        ScanLockPermissionScreen(onNaviUpClick = {}, onScanClick = {})
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun PreviewSmall() {
    FuhsingSmartLockV2AndroidTheme {
        ScanLockPermissionScreen(onNaviUpClick = {}, onScanClick = {})
    }
}