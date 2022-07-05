package com.sunion.ikeyconnect.add_lock.scan_qrcode

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.add_lock.component.CameraDeniedContent
import com.sunion.ikeyconnect.ui.component.BarcodeScan
import com.sunion.ikeyconnect.ui.component.IKeyTopAppBar
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import timber.log.Timber

@Composable
fun ScanLockQRCodeScreen(
    viewModel: ScanLockQRCodeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
//                is ScanLockQRCodeUiEvent.Complete -> navController.navigate("${AddLockRoute.Installation.route}/${it.macAddress}")
                is ScanLockQRCodeUiEvent.Complete -> navController.navigate("${AddLockRoute.Pairing.route}/${it.macAddress}")

            }
        }
    }

    val getImageLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            viewModel.setQRCodeImageUri(it)
        }

    val uiState = viewModel.uiState.collectAsState().value
    ScanLockQRCodeScreen(
        onNaviUpClick = navController::popBackStack,
        onScanResult = viewModel::setQRCodeContent,
        onFlashLightClick = viewModel::toggleTorch,
        onAlbumClick = {
            getImageLauncher.launch("image/*")
        },
        isTorchOn = uiState.isTorchOn,
        modifier = modifier
    )

    if (uiState.message.isNotEmpty()) {
        IkeyAlertDialog(
            onDismissRequest = viewModel::closeMessageDialog,
            onConfirmButtonClick = viewModel::closeMessageDialog,
            title = "",
            text = uiState.message
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanLockQRCodeScreen(
    onNaviUpClick: () -> Unit,
    onScanResult: (String) -> Unit,
    onFlashLightClick: () -> Unit,
    onAlbumClick: () -> Unit,
    isTorchOn: Boolean,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            BarcodeScanContent(
                onScanResult = onScanResult,
                onFlashLightClick = onFlashLightClick,
                onAlbumClick = onAlbumClick,
                isTorchOn = isTorchOn,
                modifier = modifier
            )
        }
        is PermissionStatus.Denied -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(id = R.dimen.space_20))
            ) {
                CameraDeniedContent(
                    shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
                    onClick = cameraPermissionState::launchPermissionRequest,
                    modifier = Modifier
                )
            }
        }
    }

    IKeyTopAppBar(
        onNaviUpClick = onNaviUpClick,
        backgroundColor = Color.Transparent,
        naviUpTint = Color.White
    )
}

@Composable
fun BarcodeScanContent(
    onScanResult: (String) -> Unit,
    onFlashLightClick: () -> Unit,
    onAlbumClick: () -> Unit,
    isTorchOn: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = dimensionResource(id = R.dimen.space_20))
    ) {
        BarcodeScan(
            onScanResult = onScanResult,
            isTorchOn = isTorchOn,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = dimensionResource(id = R.dimen.space_24))
                .padding(bottom = dimensionResource(id = R.dimen.space_24))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_frashlight),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.space_40))
                    .clickable(onClick = onFlashLightClick)
            )
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.ic_photo_library_primary),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.space_40))
                    .clickable(onClick = onAlbumClick)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewScanLockQRCodeScreen() {
    FuhsingSmartLockV2AndroidTheme {
        ScanLockQRCodeScreen(
            onNaviUpClick = {},
            onScanResult = {},
            onFlashLightClick = {},
            onAlbumClick = {},
            isTorchOn = false
        )
    }
}