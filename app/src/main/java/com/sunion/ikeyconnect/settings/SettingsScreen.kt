package com.sunion.ikeyconnect.settings

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
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
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.component.IKeyTopAppBar
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog
import com.sunion.ikeyconnect.ui.component.LoadingScreenDialog
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, navController: NavController) {
    val uiState = viewModel.uiState.collectAsState().value

    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                SettingsUiEvent.DeleteLockSuccess ->
                    navController.popBackStack(HomeRoute.Home.route, false)
                SettingsUiEvent.DeleteLockFail ->
                    Toast.makeText(context, "Cannot delete lock", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsScreen(
        onNaviUpClick = navController::popBackStack,
        onDeleteClick = viewModel::delete,
        state = uiState,
        isConnected = viewModel.isConnected,
        onEventLogClick = { thingName ->
            navController.navigate("${SettingRoute.EventLog.route}/$thingName/${uiState.wifiLock.Attributes.DeviceName}") },
    )

    if (uiState.showDeleteConfirmDialog)
        IkeyAlertDialog(
            onDismissRequest = viewModel::closeDeleteConfirmDialog,
            onConfirmButtonClick = {
                viewModel.closeDeleteConfirmDialog()
                viewModel.executeDelete()
            },
            title = stringResource(id = R.string.setting_delete_lock),
            text = stringResource(id = R.string.dialog_setting_delete_lock_confirm_message),
            confirmButtonText = stringResource(id = R.string.global_delete),
            dismissButtonText = stringResource(id = R.string.global_cancel),
            onDismissButtonClick = viewModel::closeDeleteConfirmDialog
        )

    if (uiState.isLoading)
        LoadingScreenDialog("")
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onNaviUpClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEventLogClick: (String) -> Unit,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        color = colorResource(id = R.color.primary),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        IKeyTopAppBar(
            onNaviUpClick = onNaviUpClick,
            title = stringResource(id = R.string.toolbar_setting)
        )
        IKeyDivider()
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (isConnected) {
                SettingItem {
                    Text(text = "${state.wifiLock.LockState.Battery}%", style = textStyle)
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_15)))
                    Image(
                        painter = painterResource(id = R.drawable.ic_battery_full),
                        contentDescription = null
                    )
                }
                IKeyDivider()
                SettingItemSwitch(
                    text = stringResource(id = R.string.setting_vacation_mode),
                    checked = false,
                    onCheckedChange = {},
                    textStyle = textStyle
                )
                IKeyDivider()
                SettingItemSwitch(
                    text = stringResource(id = R.string.setting_auto_lock),
                    checked = false,
                    onCheckedChange = {},
                    textStyle = textStyle
                )
                IKeyDivider()
                SettingItemSwitch(
                    text = stringResource(id = R.string.setting_keypress_beep),
                    checked = false,
                    onCheckedChange = {},
                    textStyle = textStyle
                )
                IKeyDivider()
                SettingItemSwitch(
                    text = stringResource(id = R.string.setting_secure_mode),
                    checked = false,
                    onCheckedChange = {},
                    textStyle = textStyle
                )
                IKeyDivider()
                SettingItem {
                    Text(text = stringResource(id = R.string.setting_admin_code), style = textStyle)
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(180f)
                            .size(dimensionResource(id = R.dimen.space_24))
                            .padding(4.dp)
                    )
                }
                IKeyDivider()
                SettingItem {
                    Text(text = stringResource(id = R.string.setting_event_log), style = textStyle)
                    Spacer(modifier = Modifier
                        .weight(1f)
                        )
                    Image(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(180f)
                            .size(dimensionResource(id = R.dimen.space_24))
                            .padding(4.dp)
                            .clickable(onClick = { onEventLogClick(state.macAddressOrThingName) }),
                    )
                }
                IKeyDivider()
                SettingItem {
                    Text(
                        text = stringResource(id = R.string.setting_bolt_direction),
                        style = textStyle
                    )
                }
                IKeyDivider()
                SettingItem {
                    Text(
                        text = stringResource(id = R.string.setting_wifi_setting),
                        style = textStyle
                    )
                }
                IKeyDivider()
            }

            SettingItem(
                modifier = Modifier
                    .background(colorResource(R.color.light_primary))
                    .clickable(onClick = onDeleteClick)
            ) {
                Text(text = stringResource(id = R.string.setting_delete_lock), style = textStyle)
            }

            if (isConnected) {
                IKeyDivider()
                SettingItem(
                    modifier = Modifier
                        .background(colorResource(R.color.light_primary))
                ) {
                    Text(text = stringResource(id = R.string.setting_reset), style = textStyle)
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_66)))
                Row(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28))) {
                    val style = TextStyle(color = MaterialTheme.colors.primary, fontSize = 12.sp)
                    val style1 = TextStyle(
                        color = MaterialTheme.colors.primaryVariant,
                        fontWeight = FontWeight.Light,
                        fontSize = 10.sp
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.setting_model),
                            style = style
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_9)))
                        Text(text = "??", style = style1)
                    }
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_83)))
                    Column {
                        Text(
                            text = stringResource(id = R.string.setting_firmware_version),
                            style = style
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_9)))
                        Text(text = "??", style = style1)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItemSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textStyle: TextStyle
) {
    SettingItem {
        Text(text = text, style = textStyle)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(uncheckedThumbColor = Color.White)
        )
    }
}

@Composable
private fun SettingItem(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .height(dimensionResource(id = R.dimen.space_60))
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.space_29)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Preview
@Preview(device = "id:Nexus 5")
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        SettingsScreen(onNaviUpClick = {}, onDeleteClick = {}, onEventLogClick = {}, isConnected = true, state = SettingsUiState())
        @Composable
        fun SettingsScreen(viewModel: SettingsViewModel, navController: NavController) {
            val uiState = viewModel.uiState.collectAsState().value

            val context = LocalContext.current
            LaunchedEffect(key1 = Unit) {
                viewModel.uiEvent.collect {
                    when (it) {
                        SettingsUiEvent.DeleteLockSuccess ->
                            navController.popBackStack(HomeRoute.Home.route, false)
                        SettingsUiEvent.DeleteLockFail ->
                            Toast.makeText(context, "Cannot delete lock", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            SettingsScreen(
                onNaviUpClick = navController::popBackStack,
                onDeleteClick = viewModel::delete,
                onEventLogClick = {},
                isConnected = viewModel.isConnected,
                state = SettingsUiState()
            )

            if (uiState.showDeleteConfirmDialog)
                IkeyAlertDialog(
                    onDismissRequest = viewModel::closeDeleteConfirmDialog,
                    onConfirmButtonClick = {
                        viewModel.closeDeleteConfirmDialog()
                        viewModel.executeDelete()
                    },
                    title = stringResource(id = R.string.setting_delete_lock),
                    text = stringResource(id = R.string.dialog_setting_delete_lock_confirm_message),
                    confirmButtonText = stringResource(id = R.string.global_delete),
                    dismissButtonText = stringResource(id = R.string.global_cancel),
                    onDismissButtonClick = viewModel::closeDeleteConfirmDialog
                )

            if (uiState.isLoading)
                LoadingScreenDialog("")
        }

        @Composable
        fun SettingsScreen(
            onNaviUpClick: () -> Unit,
            onDeleteClick: () -> Unit,
            isConnected: Boolean,
            modifier: Modifier = Modifier
        ) {
            val textStyle = TextStyle(
                color = colorResource(id = R.color.primary),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                IKeyTopAppBar(
                    onNaviUpClick = onNaviUpClick,
                    title = stringResource(id = R.string.toolbar_setting)
                )
                IKeyDivider()
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (isConnected) {
                        SettingItem {
                            Text(text = "?%", style = textStyle)
                            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_15)))
                            Image(
                                painter = painterResource(id = R.drawable.ic_battery_full),
                                contentDescription = null
                            )
                        }
                        IKeyDivider()
                        SettingItemSwitch(
                            text = stringResource(id = R.string.setting_vacation_mode),
                            checked = false,
                            onCheckedChange = {},
                            textStyle = textStyle
                        )
                        IKeyDivider()
                        SettingItemSwitch(
                            text = stringResource(id = R.string.setting_auto_lock),
                            checked = false,
                            onCheckedChange = {},
                            textStyle = textStyle
                        )
                        IKeyDivider()
                        SettingItemSwitch(
                            text = stringResource(id = R.string.setting_keypress_beep),
                            checked = false,
                            onCheckedChange = {},
                            textStyle = textStyle
                        )
                        IKeyDivider()
                        SettingItemSwitch(
                            text = stringResource(id = R.string.setting_secure_mode),
                            checked = false,
                            onCheckedChange = {},
                            textStyle = textStyle
                        )
                        IKeyDivider()
                        SettingItem {
                            Text(text = stringResource(id = R.string.setting_admin_code), style = textStyle)
                            Spacer(modifier = Modifier.weight(1f))
                            Image(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = null,
                                modifier = Modifier
                                    .rotate(180f)
                                    .size(dimensionResource(id = R.dimen.space_24))
                                    .padding(4.dp)
                            )
                        }
                        IKeyDivider()
                        SettingItem {
                            Text(
                                text = stringResource(id = R.string.setting_bolt_direction),
                                style = textStyle
                            )
                        }
                        IKeyDivider()
                        SettingItem {
                            Text(
                                text = stringResource(id = R.string.setting_wifi_setting),
                                style = textStyle
                            )
                        }
                        IKeyDivider()
                    }

                    SettingItem(
                        modifier = Modifier
                            .background(colorResource(R.color.light_primary))
                            .clickable(onClick = onDeleteClick)
                    ) {
                        Text(text = stringResource(id = R.string.setting_delete_lock), style = textStyle)
                    }

                    if (isConnected) {
                        IKeyDivider()
                        SettingItem(
                            modifier = Modifier
                                .background(colorResource(R.color.light_primary))
                        ) {
                            Text(text = stringResource(id = R.string.setting_reset), style = textStyle)
                        }
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_66)))
                        Row(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28))) {
                            val style = TextStyle(color = MaterialTheme.colors.primary, fontSize = 12.sp)
                            val style1 = TextStyle(
                                color = MaterialTheme.colors.primaryVariant,
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp
                            )
                            Column {
                                Text(
                                    text = stringResource(id = R.string.setting_model),
                                    style = style
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_9)))
                                Text(text = "??", style = style1)
                            }
                            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_83)))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.setting_firmware_version),
                                    style = style
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_9)))
                                Text(text = "??", style = style1)
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun SettingItemSwitch(
            text: String,
            checked: Boolean,
            onCheckedChange: (Boolean) -> Unit,
            textStyle: TextStyle
        ) {
            SettingItem {
                Text(text = text, style = textStyle)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(uncheckedThumbColor = Color.White)
                )
            }
        }

        @Composable
        fun SettingItem(
            modifier: Modifier = Modifier,
            content: @Composable RowScope.() -> Unit
        ) {
            Row(
                modifier = modifier
                    .height(dimensionResource(id = R.dimen.space_60))
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.space_29)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }

        @Preview
        @Preview(device = "id:Nexus 5")
        @Composable
        fun Preview() {
            FuhsingSmartLockV2AndroidTheme {
                SettingsScreen(onNaviUpClick = {}, onDeleteClick = {}, isConnected = true)
            }
        }

        @Preview
        @Composable
        fun PreviewDisconnected() {
            FuhsingSmartLockV2AndroidTheme {
                SettingsScreen(onNaviUpClick = {}, onDeleteClick = {}, isConnected = false)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewDisconnected() {
    FuhsingSmartLockV2AndroidTheme {
        SettingsScreen(onNaviUpClick = {}, onDeleteClick = {}, onEventLogClick = {}, isConnected = false, state = SettingsUiState())
    }
}