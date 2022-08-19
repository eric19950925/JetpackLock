package com.sunion.ikeyconnect.auto_unlock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.account_management.AccountScreen
import com.sunion.ikeyconnect.add_lock.admin_code.AdminCodeViewModel
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.RegistryGetResponse
import com.sunion.ikeyconnect.settings.SettingItem
import com.sunion.ikeyconnect.settings.SettingItemSwitch
import com.sunion.ikeyconnect.settings.SettingsUiState
import com.sunion.ikeyconnect.ui.component.BackTopAppBar
import com.sunion.ikeyconnect.ui.component.FunctionRow
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18
import com.sunion.ikeyconnect.ui.theme.colorPrimaryMediumSize16

@Composable
fun AutoUnlockScreen(viewModel: AutoUnlockViewModel, navController: NavController) {
    val uiState = viewModel.uiState.collectAsState().value
//    val tempUiState = viewModel.newRegistryAttributes.collectAsState().value
    BackHandler {
        viewModel.leavePage { navController.popBackStack() }
        return@BackHandler
    }
    AutoUnlockScreen(
        onNaviUpClick = { viewModel.leavePage { navController.popBackStack() } },
        state = uiState,
        tempState = uiState,
        onAutoUnLockClick = {},
        onEnterNotifyClick = {},
        onLeaveNotifyClick = {},
        onSetLocationClick = {}
    )
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
                checked = false,
                onCheckedChange = { onAutoUnLockClick(false) },
                textStyle = textStyle
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
                text = "Not enabled",
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
                onCheckedChange = { onAutoUnLockClick(false) },
                textStyle = textStyle
            )
            SettingItemSwitch(
                text = stringResource(id = R.string.auto_un_lock_leave_switch),
                checked = false,
                onCheckedChange = { onAutoUnLockClick(false) },
                textStyle = textStyle
            )
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
