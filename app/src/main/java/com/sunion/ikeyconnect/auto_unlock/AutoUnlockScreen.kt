package com.sunion.ikeyconnect.auto_unlock

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.MainActivity
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.model.DeviceToken
import com.sunion.ikeyconnect.settings.SettingItem
import com.sunion.ikeyconnect.settings.SettingItemSwitch
import com.sunion.ikeyconnect.ui.component.BackTopAppBar
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun AutoUnlockScreen(viewModel: AutoUnlockViewModel, navController: NavController) {
    val uiState = viewModel.uiState.collectAsState().value
//    val tempUiState = viewModel.newRegistryAttributes.collectAsState().value
    val context = LocalContext.current
    BackHandler {
        viewModel.leavePage { navController.popBackStack() }
        return@BackHandler
    }
    AutoUnlockScreen(
        onNaviUpClick = { viewModel.leavePage { navController.popBackStack() } },
        state = uiState,
        tempState = uiState,
        onAutoUnLockClick = {
            (context as MainActivity).performPendingGeofenceTask(it, viewModel.deviceIdentity)
//            viewModel.onAutoUnLockClick()
        },
        onEnterNotifyClick = {},
        onLeaveNotifyClick = {},
        onSetLocationClick = { navController.navigate("${AutoUnLockRoute.SettingLocation.route}/${viewModel.deviceIdentity}/${viewModel.deviceType}/${viewModel.uiState.value.initLocation.latitude}/${viewModel.uiState.value.initLocation.longitude}") }
    )
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is AutoUnlockUiEvent.SetAutoUnlockFail ->
                    Toast.makeText(context, "There's an error setting auto unlock", Toast.LENGTH_SHORT).show()
                is AutoUnlockUiEvent.SetAutoUnlockSuccess ->
                    Toast.makeText(context, "Setting auto unlock success.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

@Composable
fun AutoUnlockScreen(
    onNaviUpClick: () -> Unit,
    onAutoUnLockClick: (Boolean) -> Unit,
    onEnterNotifyClick: (Boolean) -> Unit,
    onLeaveNotifyClick: (Boolean) -> Unit,
    onSetLocationClick: () -> Unit,
    state: AutoUnlockUiState,
    tempState: AutoUnlockUiState,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        color = colorResource(id = R.color.black),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
    Scaffold(topBar = {
        BackTopAppBar(
            onNaviUpClick = onNaviUpClick,
            title = stringResource(id = R.string.lock_tab_bar_auto_unlock)
        )
    }, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(it)
        ) {
            IKeyDivider(modifier = Modifier.fillMaxWidth())

            SettingItemSwitch(
                text = stringResource(id = R.string.lock_tab_bar_auto_unlock),
                checked = state.isAutoUnLockChecked,
                onCheckedChange = { isChecked ->
                    onAutoUnLockClick(isChecked)
                                  },
                textStyle = textStyle,
                isClickable = state.isAutoUnLockClickable
            )
            Row (modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.auto_un_lock_description),
                    style = TextStyle(color = colorResource(id = R.color.disconnected)),
                    modifier = Modifier
                        .wrapContentWidth()
                        .weight(1f)
                        .padding(start = dimensionResource(id = R.dimen.space_29))
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_faq),
                    contentDescription = "faq",
                    modifier = Modifier
                        .size(20.dp)
                        .weight(0.2f)
                        .align(CenterVertically)
                        .clickable {

                        }
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_15)))
            Text(
                text = stringResource(id = R.string.auto_unlock_location_permission_status_text),
                style = TextStyle(color = colorResource(id = R.color.log_error)),
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.space_29))
            )
            Text(
                text = if(state.phonePermission) stringResource(id = R.string.global_enabled) else stringResource(id = R.string.global_not_enabled),
                style = TextStyle(color = colorResource(id = R.color.log_error)),
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.space_29))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_15)))
            IKeyDivider()
            SettingItemSwitch(
                text = stringResource(id = R.string.auto_un_lock_enter_switch),
                checked = false,
                onCheckedChange = { onEnterNotifyClick(false) },
                textStyle = textStyle,
                isClickable = true
            )
            SettingItemSwitch(
                text = stringResource(id = R.string.auto_un_lock_leave_switch),
                checked = false,
                onCheckedChange = { onLeaveNotifyClick(false) },
                textStyle = textStyle,
                isClickable = true
            )
            if(state.lockPermission == DeviceToken.PERMISSION_ALL){
                SettingItem {
                    Text(text = stringResource(id = R.string.toolbar_title_lock_location), style = textStyle)
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(180f)
                            .size(dimensionResource(id = R.dimen.space_24))
                            .padding(4.dp)
                            .clickable { onSetLocationClick.invoke() }
                    )
                }
            }
        }
    }
}
@Preview
@Composable
private fun PreviewAutoUnlockScreen() {
    FuhsingSmartLockV2AndroidTheme {
        AutoUnlockScreen(
            onAutoUnLockClick = { },
            onEnterNotifyClick = { },
            onLeaveNotifyClick = { },
            onSetLocationClick = { },
            onNaviUpClick = { },
            state = AutoUnlockUiState(),
            tempState = AutoUnlockUiState(),
        )
    }
}
