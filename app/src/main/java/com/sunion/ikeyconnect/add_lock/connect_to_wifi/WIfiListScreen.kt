package com.sunion.ikeyconnect.add_lock.connect_to_wifi

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // unauto
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.ui.component.ScreenScaffoldWithTopAppBar
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

data class WifiInfo(val ssid: String, val needPassword: Boolean, val signalLevel: Int)

@Composable
fun WIfiListScreen(viewModel: WifiListViewModel, navController: NavController) {
    val uiState = viewModel.uiState.collectAsState().value
    WIfiListScreen(
        onNaviUpClick = { navController.popBackStack() },
        state = uiState,
        onSkipClick = {
//            viewModel.setSkip(onComplete = {
                navController.navigate("${AddLockRoute.AdminCode.route}/${viewModel.macAddress}")
//            })
        },
        onItemClick = { ssid ->
            navController.navigate("${AddLockRoute.ConnectWifi.route}/${viewModel.macAddress}/$ssid")
        },
        onScanClick = viewModel::scanWIfi
    )

    if (uiState.showDisconnect)
        DisconnectionDialog(onConfirmButtonClick = {
            navController.popBackStack(HomeRoute.Home.route, false)
        })
}

@Composable
fun WIfiListScreen(
    onNaviUpClick: () -> Unit,
    state: WiFiListUiState,
    onSkipClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffoldWithTopAppBar(
        onNaviUpClick = onNaviUpClick,
        title = stringResource(id = R.string.connect_to_wifi_title),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_18)))
        Text(
            text = stringResource(id = R.string.connect_to_wifi_select_wifi_network),
            style = TextStyle(color = MaterialTheme.colors.primary, fontSize = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.space_28))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))
        Text(
            text = stringResource(id = R.string.connect_to_wifi_available_networks),
            style = TextStyle(color = Color(0xFF7496AF), fontSize = 12.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.space_28))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_7)))
        IKeyDivider()
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.wifiList) {
                ConnectToWifiItem(it, onItemClick)
                IKeyDivider()
            }
            item {
                if (state.isScanning)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .wrapContentSize()
                    )
                if (state.wifiList.isEmpty() && !state.isScanning)
                    Button(onClick = onScanClick, shape = RoundedCornerShape(50)) {
                        Text(text = stringResource(id = R.string.global_scan))
                    }
            }
        }
        Text(
            text = stringResource(id = R.string.connect_to_wifi_skip),
            style = TextStyle(
                color = MaterialTheme.colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .height(40.dp)
                .width(200.dp)
                .clickable(onClick = onSkipClick)
                .wrapContentSize()
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))
    }
}

@Composable
fun ConnectToWifiItem(
    wifiInfo: WifiInfo,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.space_59))
            .padding(horizontal = dimensionResource(id = R.dimen.space_28))
            .clickable(onClick = { onClick(wifiInfo.ssid) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = wifiInfo.ssid,
            style = TextStyle(
                color = MaterialTheme.colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_20)))
        Image(
            painter = painterResource(id = R.drawable.ic_wifi_lock),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_10)))
        Image(
            painter = painterResource(id = R.drawable.ic_wifi),
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun PreviewConnectToWIfiScreen() {
    FuhsingSmartLockV2AndroidTheme {
        WIfiListScreen(
            onNaviUpClick = {},
            state = WiFiListUiState(
                listOf(
                    WifiInfo("Home", true, 5),
                    WifiInfo("Office", true, 5),
                    WifiInfo("CHTWifi", true, 5),
                ),
                true
            ),
            onSkipClick = {},
            onItemClick = {},
            onScanClick = {}
        )
    }
}


@Preview
@Composable
private fun PreviewConnectToWIfiScreen2() {
    FuhsingSmartLockV2AndroidTheme {
        WIfiListScreen(
            onNaviUpClick = {},
            state = WiFiListUiState(),
            onSkipClick = {},
            onItemClick = {},
            onScanClick = {}
        )
    }
}