package com.sunion.ikeyconnect.settings.wifi_setting

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.component.AddLockTopAppBar
import com.sunion.ikeyconnect.add_lock.component.EnableBluetoothDialog
import com.sunion.ikeyconnect.add_lock.component.ExitPromptDialog
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun WiFiSettingPairingScreen(
    viewModel: WiFiSettingViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val locationPermissionRequest =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                }
                else -> {
                    // No location access granted.
                }
            }
        }

    LaunchedEffect(key1 = Unit) {
        locationPermissionRequest.launch(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            else
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
        )
    }

    val uiState = viewModel.uiState.collectAsState().value

    val bluetoothEnableLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            viewModel.checkIsBluetoothEnable()
        }

    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        viewModel.checkIsBluetoothEnable()

        viewModel.uiEvent.collect {
            when (it) {
                is PairingUiEvent.UserNoPermissionAccessLock ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.user_no_permission_access_lock_snack),
                        Toast.LENGTH_SHORT
                    ).show()
                PairingUiEvent.DeleteLockFail -> Unit
                PairingUiEvent.DeleteLockSuccess ->
                    navController.popBackStack()
            }
        }
    }

//    BackHandler {
//        viewModel.closeExitPromptDialog()
//    }

    WiFiSettingPairingScreen(
        onNaviUpClick = { navController.popBackStack() /*viewModel.showExitPromptDialog()*/ },
        state = uiState,
        onStartClick = {
            if (uiState.connectionState == PairingUiState.ConnectionState.Done) {
                if (uiState.shouldShowNext)
                    navController.navigate("${WiFiSettingRoute.WifiList.route}/${viewModel.macAddress}")
                else
                    navController.navigate(HomeRoute.Home.route)
            } else
                viewModel.startPairing()
        },
        modifier = modifier
    )

    if (uiState.shouldShowExitDialog)
        ExitPromptDialog(
            onDismissRequest = viewModel::closeExitPromptDialog,
            onConfirmButtonClick = {
                viewModel.closeExitPromptDialog()
                viewModel.deleteLock()
            },
            onDismissButtonClick = viewModel::closeExitPromptDialog
        )

    if (uiState.shouldShowBluetoothEnableDialog)
        EnableBluetoothDialog(
            onDismissRequest = viewModel::closeBluetoothEnableDialog,
            onConfirmButtonClick = {
                viewModel.closeBluetoothEnableDialog()
                bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
            onDismissButtonClick = viewModel::closeBluetoothEnableDialog,
        )
}

@Composable
fun WiFiSettingPairingScreen(
    onNaviUpClick: () -> Unit,
    state: PairingUiState,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AddLockTopAppBar(
            title = stringResource(id = R.string.toolbar_title_add_lock),
            onNaviUpClick = onNaviUpClick,
            step = "1",
            totalStep = "3"
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
        Image(
            painter = painterResource(id = R.drawable.ble_pairing),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
        val style = TextStyle(color = MaterialTheme.colors.primaryVariant, fontSize = 14.sp)
        Text(
            text = stringResource(id = R.string.start_pairing_label),
            style = style,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.space_56))
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.global_bluetooth),
                style = if (state.connectionState == PairingUiState.ConnectionState.Done)
                    style.copy(color = MaterialTheme.colors.primary)
                else style
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_40)))
            Image(
                painter = painterResource(
                    id = if (state.connectionState == PairingUiState.ConnectionState.Done
                        || state.connectionState == PairingUiState.ConnectionState.Error
                    ) R.drawable.ic_progress_dots
                    else R.drawable.ic_progress_dots_grey
                ),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_40)))
            ConnectStateImage(state.connectionState)
        }

        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(
                id = when (state.connectionState) {
                    PairingUiState.ConnectionState.Error -> R.string.global_reconnect
                    PairingUiState.ConnectionState.Done -> {
                        if (state.shouldShowNext) R.string.global_next
                        else R.string.global_complete
                    }
                    else -> R.string.global_start_uppercase
                }
            ),
            onClick = onStartClick,
            enabled = state.isBlueToothAvailable && state.connectionState != PairingUiState.ConnectionState.Processing
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_40)))
    }
}

@Composable
private fun ConnectStateImage(connectionState: PairingUiState.ConnectionState) {
    if (connectionState == PairingUiState.ConnectionState.Processing)
        CircularProgressIndicator(modifier = Modifier.size(dimensionResource(id = R.dimen.space_24)))
    else
        Image(
            painter = painterResource(
                id = when (connectionState) {
                    PairingUiState.ConnectionState.Error -> R.drawable.ic_error
                    PairingUiState.ConnectionState.Done -> R.drawable.ic_done
                    else -> R.drawable.ic_done_grey
                }
            ),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = R.dimen.space_24))
        )
}

@Preview(device = Devices.NEXUS_5X)
@Composable
private fun PreviewAddLockScreenSmall(@PreviewParameter(AddLockScreenPreviewParameterProvider::class) state: PairingUiState) {
    FuhsingSmartLockV2AndroidTheme {
        WiFiSettingPairingScreen(
            onNaviUpClick = { },
            state = state,
            onStartClick = { }
        )
    }
}

class AddLockScreenPreviewParameterProvider : PreviewParameterProvider<PairingUiState> {
    override val values = sequenceOf(
        PairingUiState(),
        PairingUiState(isBlueToothAvailable = true),
        PairingUiState(
            isBlueToothAvailable = true,
            connectionState = PairingUiState.ConnectionState.Processing
        ),
        PairingUiState(
            isBlueToothAvailable = true,
            connectionState = PairingUiState.ConnectionState.Error
        ),
        PairingUiState(
            isBlueToothAvailable = true,
            connectionState = PairingUiState.ConnectionState.Done,
            shouldShowNext = true
        ),
        PairingUiState(
            isBlueToothAvailable = true,
            connectionState = PairingUiState.ConnectionState.Done,
            shouldShowNext = false
        )
    )
}