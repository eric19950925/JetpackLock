package com.sunion.ikeyconnect.add_lock.connect_to_wifi

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.ui.component.InputTextField
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.component.ScreenScaffoldWithTopAppBar
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18

@Composable
fun ConnectWifiScreen(viewModel: ConnectWifiViewModel, navController: NavController) {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                ConnectWifiUiEvent.ConnectSuccess ->
                    navController.navigate("${AddLockRoute.LockOverview.route}/${viewModel.macAddress}")
//                    navController.navigate("${AddLockRoute.AdminCode.route}/${viewModel.macAddress}")
                ConnectWifiUiEvent.ConnectFailed ->
                    navController.navigate("${AddLockRoute.Pairing.route}/${viewModel.macAddress}")
                ConnectWifiUiEvent.BleDisconnected ->
                    Toast.makeText(context,
                        context.getString(R.string.global_disconnect),
                        Toast.LENGTH_SHORT).show()
                ConnectWifiUiEvent.BleConnecting ->
                    Toast.makeText(context,
                        "Connecting",
                        Toast.LENGTH_SHORT).show()
                ConnectWifiUiEvent.ResetWifi -> {
                    navController.navigate("${AddLockRoute.Pairing.route}/${viewModel.macAddress}")
                }
            }
        }
    }

    val uiState = viewModel.uiState.collectAsState().value

    if (uiState.isProgress)
        ConnectProgressScreen(uiState.progressMessage)
    else
        ConnectWifiScreen(
            onNaviUpClick = { navController.popBackStack() },
            ssid = viewModel.ssid ?: "",
            wifiPassword = uiState.password,
            onWifiPasswordChange = viewModel::setWifiPassword,
            onConnectClick = viewModel::connectToWifi,
            passwordError = uiState.errorMessage
        )

    if (uiState.showDisconnect)
        DisconnectionDialog(onConfirmButtonClick = {
            navController.popBackStack(HomeRoute.Home.route, false)
        })
}

@Composable
fun ConnectWifiScreen(
    onNaviUpClick: () -> Unit,
    ssid: String,
    wifiPassword: String,
    onWifiPasswordChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    passwordError: String,
    modifier: Modifier = Modifier,
) {
    ScreenScaffoldWithTopAppBar(
        onNaviUpClick = onNaviUpClick,
        title = stringResource(id = R.string.connect_to_wifi_title),
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_18)))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(id = R.dimen.space_28))
        ) {
            Text(
                text = stringResource(id = R.string.connect_to_wifi_enter_your_password),
                style = MaterialTheme.typography.colorPrimaryBoldSize18
            )
            Spacer(
                modifier = Modifier.height(dimensionResource(id = R.dimen.space_10))
            )
            val style = TextStyle(color = Color(0xFF7496AF), fontSize = 12.sp)
            Text(
                text = stringResource(id = R.string.connect_to_wifi_please_enter_your_wifi_password),
                style = style
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_30)))
            Row {
                Image(
                    painter = painterResource(id = R.drawable.ic_wifi),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.space_20))
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_7)))
                Text(
                    text = ssid,
                    style = MaterialTheme.typography.colorPrimaryBoldSize18.copy(fontSize = 16.sp)
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_28)))
            InputTextField(
                title = stringResource(id = R.string.connect_to_wifi_password),
                value = wifiPassword,
                onValueChange = onWifiPasswordChange,
                error = passwordError,
                titleTextStyle = style.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_44)))
            PrimaryButton(
                text = stringResource(id = R.string.connect_to_wifi_connect),
                onClick = onConnectClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEnterWifiPasswordScreen() {
    FuhsingSmartLockV2AndroidTheme {
        ConnectWifiScreen(
            onNaviUpClick = {},
            ssid = "ssid",
            wifiPassword = "",
            onWifiPasswordChange = {},
            onConnectClick = { },
            passwordError = ""
        )
    }
}