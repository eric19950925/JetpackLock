package com.sunion.ikeyconnect.add_lock.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.home.HomeTestRoute
import kotlinx.coroutines.flow.flow

@Composable
fun PairingScreen(viewModel: PairingViewModel,
                    navController: NavController,
                    modifier: Modifier = Modifier
) {

    val uiState = viewModel.uiState.collectAsState().value
    PairingScreen(
        on_C0C1_Click = { viewModel.startPairing() },
        state = uiState,
        on_next_Click = {
//            if (uiState.connectionState == PairingUiState.ConnectionState.Done) {
//                if (uiState.shouldShowNext)
                    navController.navigate(
                        if (uiState.lockIsWifi)
                            "${HomeTestRoute.WifiList.route}/${viewModel.macAddress}"
//                            "${AddLockRoute.WifiList.route}/${viewModel.macAddress}"
                        else
                            "${AddLockRoute.AdminCode.route}/${viewModel.macAddress}"
                    )
//                else
//                    navController.navigate(HomeRoute.Home.route)
//            } else
//                viewModel.startPairing()
        },
        on_getThingName_Click = {
            viewModel.getThingName()
        },
        on_lock_Click = {
            viewModel.setLock()
        },
        modifier = modifier
    )

//    if (uiState.shouldShowExitDialog)
//        ExitPromptDialog(
//            onDismissRequest = viewModel::closeExitPromptDialog,
//            onConfirmButtonClick = {
//                viewModel.closeExitPromptDialog()
//                viewModel.deleteLock()
//            },
//            onDismissButtonClick = viewModel::closeExitPromptDialog
//        )

//    if (uiState.shouldShowBluetoothEnableDialog)
//        EnableBluetoothDialog(
//            onDismissRequest = viewModel::closeBluetoothEnableDialog,
//            onConfirmButtonClick = {
//                viewModel.closeBluetoothEnableDialog()
//                bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
//            },
//            onDismissButtonClick = viewModel::closeBluetoothEnableDialog,
//        )

}
@Composable
fun PairingScreen(
    on_C0C1_Click: () -> Unit,
    state: PairingUiState,
    on_next_Click: () -> Unit,
    on_getThingName_Click: () -> Unit,
    on_lock_Click: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(55.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_C0C1_Click
            ){ Text("C0 C1") }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_next_Click,
                colors = if (state.connectionState == PairingUiState.ConnectionState.Done)
                    ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                else ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant)
            ){ Text("next") }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_getThingName_Click,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ){ Text("getThingName") }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_lock_Click,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ){ Text("lock") }
        }
    }
}
@Preview(showSystemUi = true)
@Composable
private fun Preview(@PreviewParameter(AddLockScreenPreviewParameterProvider::class) state: PairingUiState) {
    PairingScreen(
        on_C0C1_Click = {},
        state = state,
        on_next_Click = {},
        on_getThingName_Click = {},
        on_lock_Click = {}
        )
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