package com.sunion.ikeyconnect.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.data.getFirmwareModelUrlByString
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.RegistryGetResponse
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.home.HomeViewModel
import com.sunion.ikeyconnect.ui.component.*
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, navController: NavController) {
    val uiState = viewModel.uiState.collectAsState().value
    val tempUiState = viewModel.newRegistryAttributes.collectAsState().value

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
    BackHandler {
        viewModel.leaveSettingPage { navController.popBackStack() }
        return@BackHandler
    }
    SettingsScreen(
        onNaviUpClick = {
            viewModel.leaveSettingPage { navController.popBackStack() }
        },
        onDeleteClick = viewModel::delete,
        onEditClick = { isClicked ->
            viewModel.editClick(isClicked)
        },
        onVacationModeClick = { isEnable ->
            viewModel.setVacationMode(isEnable)
        },
        onAutoLockClick = { isEnable ->
            viewModel.setAutoLock(isEnable)
        },
        onAutoLockDone = { delay ->
            viewModel.onAutoLockDone(delay)
        },
        onKeypressBeepClick = { isEnable ->
            viewModel.setKeyPressBeep(isEnable)
        },
        onSecureModeClick = { isEnable ->
            viewModel.setScureMode(isEnable)
        },
        onChangeAdminCodeClick = { thingName ->
            viewModel.leaveSettingPage {
            navController.navigate("${SettingRoute.ChangeAdminCode.route}/$thingName/${viewModel.deviceType}")}
        },
        onResetBoltDirectionClick = {},
        onWiFiSettingClick = {
            viewModel.leaveSettingPage {
                navController.navigate("${SettingRoute.WiFiSetting.route}/" +
                        "${viewModel.deviceIdentity}/" +
                        "${uiState.registryAttributes.bluetooth?.macAddress}/" +
                        "${uiState.registryAttributes.bluetooth?.broadcastName}/" +
                        "${uiState.registryAttributes.bluetooth?.connectionKey}/" +
                        "${uiState.registryAttributes.bluetooth?.shareToken}"
                ) }
        },
        onFactoryResetClick = viewModel::onFactoryResetClick,
        state = uiState,
        tempState = tempUiState,
        isConnected = viewModel.isConnected,
        onEventLogClick = { thingName ->
            viewModel.leaveSettingPage {
            navController.navigate("${SettingRoute.EventLog.route}/$thingName/${uiState.registryAttributes.deviceName}/${uiState.deviceType}") }
                          },
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

    if (uiState.showFactoryResetDialog)
        IkeyAlertEditDialog(
            onDismissRequest = viewModel::closeFactoryResetDialog,
            onConfirmButtonClick = {
                viewModel.closeFactoryResetDialog()
                viewModel.onFactoryResetDialogComfirm()
            },
            title = stringResource(id = R.string.setting_reset),
            text = stringResource(id = R.string.setting_factory_reset_alert_content),
            confirmButtonText = stringResource(id = R.string.global_reset),
            dismissButtonText = stringResource(id = R.string.global_cancel),
            onDismissButtonClick = viewModel::closeFactoryResetDialog,
            textFieldSubTitle = stringResource(id = R.string.global_admin_code),
            textFieldValue = viewModel.adminCode.value,
            onTextFieldChange = viewModel::setAdminCode,
        )

    if (uiState.isLoading)
        LoadingScreenDialog("")
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    tempState: RegistryGetResponse.RegistryPayload.RegistryAttributes?,
    onNaviUpClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: (Boolean) -> Unit,
    onEventLogClick: (String) -> Unit,
    onVacationModeClick: (Boolean) -> Unit,
    onAutoLockClick: (Boolean) -> Unit,
    onAutoLockDone: (Int?) -> Unit,
    onKeypressBeepClick: (Boolean) -> Unit,
    onSecureModeClick: (Boolean) -> Unit,
    onChangeAdminCodeClick: (String) -> Unit,
    onResetBoltDirectionClick: (Boolean) -> Unit,
    onWiFiSettingClick: () -> Unit,
    onFactoryResetClick: () -> Unit,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getFirmwareModelUrlByString(tempState?.model?:"")))
    val context = LocalContext.current

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
                    Text(text = "${state.battery}%", style = textStyle)
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_15)))
                    Image(
                        painter = painterResource(id = R.drawable.ic_battery_full),
                        contentDescription = null
                    )
                }
                IKeyDivider()
                SettingItemSwitch(
                    text = stringResource(id = R.string.setting_vacation_mode),
                    checked = tempState?.vacationMode?:false,
                    onCheckedChange = { onVacationModeClick(!(tempState?.vacationMode?:false)) },
                    textStyle = textStyle,
                    isClickable = true
                )
                IKeyDivider()
                SettingAutoLockItem(
                    checked = tempState?.autoLock?:false,
                    isEditable = state.isAutoLockEditClicked,
                    onEditClick = { onEditClick(state.isAutoLockEditClicked) },
                    onCheckedChange = { onAutoLockClick(!(tempState?.autoLock?:false)) },
                    onAutoLockDone = { delay -> onAutoLockDone(delay) },
                    autoLockDelay = tempState?.autoLockDelay?:2,
                    textStyle = textStyle
                )
                IKeyDivider()
                SettingItemSwitch(
                    text = stringResource(id = R.string.setting_keypress_beep),
                    checked = tempState?.keyPressBeep?:false,
                    onCheckedChange = { onKeypressBeepClick(!(tempState?.keyPressBeep?:false)) },
                    textStyle = textStyle,
                    isClickable = true
                )
                if(state.deviceType.equals(HomeViewModel.DeviceType.WiFi.typeNum)){
                    IKeyDivider()
                    SettingItemSwitch(
                        text = stringResource(id = R.string.setting_secure_mode),
                        checked = tempState?.secureMode?:false,
                        onCheckedChange = { onSecureModeClick(!(tempState?.secureMode?:false)) },
                        textStyle = textStyle,
                        isClickable = true
                    )
                }
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
                            .clickable { onChangeAdminCodeClick(state.macAddressOrThingName) }
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
                if(state.deviceType.equals(HomeViewModel.DeviceType.WiFi.typeNum)){
                    IKeyDivider()
                    SettingItem {
                        Text(
                            text = stringResource(id = R.string.setting_wifi_setting),
                            style = textStyle
                        )
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
                                .clickable(onClick = { onWiFiSettingClick() }),
                        )
                    }
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
            if(isConnected) {
                IKeyDivider()
                SettingItem(
                    modifier = Modifier
                        .background(colorResource(R.color.light_primary))
                        .clickable { onFactoryResetClick() }
                ) {
                    Text(text = stringResource(id = R.string.setting_reset), style = textStyle)
                }
            }
            IKeyDivider()
            SettingItem(
                modifier = Modifier
                    .background(colorResource(R.color.light_primary))
                    .clickable {
                        try {
                            startActivity(context, intent, null)
                        } catch (error: Throwable) {
                            Timber.d(error)
                        }
                    }
            ) {
                Text(text = stringResource(id = R.string.support_faq), style = textStyle)
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))

            if (isConnected) {
                Row(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28))) {
                    val style = TextStyle(color = MaterialTheme.colors.primary, fontSize = 12.sp)
                    val style1 = TextStyle(
                        color = MaterialTheme.colors.primaryVariant,
                        fontWeight = FontWeight.Light,
                        fontSize = 10.sp
                    )
//                    Column {
//                        Text(
//                            text = stringResource(id = R.string.setting_model),
//                            style = style
//                        )
//                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_9)))
//                        Text(text = state.registryAttributes.model, style = style1)
//                    }
//                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_83)))
                    Column {
                        Text(
                            text = stringResource(id = R.string.setting_firmware_version),
                            style = style
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_9)))
                        Text(text = state.registryAttributes.firmwareVersion, style = style1)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun SettingAutoLockItem(
    checked: Boolean,
    isEditable: Boolean,
    onEditClick: () -> Unit,
    onCheckedChange: () -> Unit,
    onAutoLockDone: (Int?) -> Unit,
    autoLockDelay: Int,
    textStyle: TextStyle
) {
    var autoLockDelayState by remember { mutableStateOf<Int?>(null) }

    Column {
        SettingItem {
            Text(text = stringResource(id = R.string.setting_auto_lock), style = textStyle)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
                colors = SwitchDefaults.colors(uncheckedThumbColor = Color.White)
            )
        }
        Text(
            text = stringResource(id = R.string.setting_auto_lock_description),
            style = TextStyle(color = colorResource(id = R.color.popup_text)),
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = dimensionResource(id = R.dimen.space_29))
        )
        if(checked && !isEditable){
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_15)))
            Row(modifier = Modifier
                .fillMaxSize()
                .padding(start = dimensionResource(id = R.dimen.space_29)), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${stringResource(id = R.string.setting_auto_lock_time)} ${
                        if (autoLockDelay < 6) { "${autoLockDelay.times(10)} s" 
                        } else { 
                            "${autoLockDelay.times(10).div(60)} m ${autoLockDelay.times(10) % 60} s" }
                    }",
                    style = textStyle
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_15)))
                Image(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.space_26))
                        .clickable { onEditClick() },
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = null
                )
            }
        }
        if(isEditable){
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.space_10),
                        end = dimensionResource(id = R.dimen.space_10)
                    ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(id = R.string.global_cancel), style = textStyle, modifier = Modifier.clickable { onEditClick() })
                    Text(text = stringResource(id = R.string.global_done), style = textStyle, modifier = Modifier.clickable { onEditClick(); onAutoLockDone(autoLockDelayState) })
                }
                AndroidView(
                    modifier = Modifier
                        .padding(
                            start = dimensionResource(id = R.dimen.space_80),
                            end = dimensionResource(id = R.dimen.space_80)
                        )
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    factory = { context ->
                        context.setTheme(R.style.ThemeOverlay_NumberPicker)
                        NumberPicker(context).apply {
                            minValue = 1
                            maxValue = 90
                            value = autoLockDelay
                            textSize = 40f

                            setFormatter { value ->
                                if (value < 6) {
                                    "${value.times(10)} s"
                                } else {
                                    "${value.times(10).div(60)} m ${value.times(10) % 60} s"
                                }
                            }
//                            textColor = resources.getColor(R.color.primary)
                            setOnValueChangedListener { numberPicker, _, selectNum ->
                                Timber.d("selectNum = $selectNum")
                                autoLockDelayState = selectNum
                            }
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))

    }
}


@Composable
fun SettingItemSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isClickable: Boolean,
    textStyle: TextStyle
) {
    SettingItem {
        Text(text = text, style = textStyle)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(uncheckedThumbColor = Color.White),
            enabled = isClickable
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

@RequiresApi(Build.VERSION_CODES.Q)
@Preview
@Preview(device = "id:Nexus 5")
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        SettingsScreen(
            onNaviUpClick = {}, onDeleteClick = {}, onEditClick = {}, onEventLogClick = {}, onAutoLockDone = {}, onVacationModeClick = {},
            onAutoLockClick = {}, onKeypressBeepClick = {}, onSecureModeClick = {}, onChangeAdminCodeClick = {},
            onResetBoltDirectionClick = {}, onWiFiSettingClick = {}, onFactoryResetClick = {}, isConnected = true, state = SettingsUiState(), tempState = null)
        @RequiresApi(Build.VERSION_CODES.Q)
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
                onDeleteClick = viewModel::delete, onEditClick = {}, onAutoLockDone = {},
                onEventLogClick = {}, onVacationModeClick = {},
                onAutoLockClick = {}, onKeypressBeepClick = {}, onSecureModeClick = {}, onChangeAdminCodeClick = {},
                onResetBoltDirectionClick = {}, onWiFiSettingClick = {}, onFactoryResetClick = {},
                isConnected = viewModel.isConnected,
                state = SettingsUiState(), tempState = null
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
                            textStyle = textStyle,
                            isClickable = true
                        )
                        IKeyDivider()
                        SettingItemSwitch(
                            text = stringResource(id = R.string.setting_auto_lock),
                            checked = false,
                            onCheckedChange = {},
                            textStyle = textStyle,
                            isClickable = true
                        )
                        IKeyDivider()
                        SettingItemSwitch(
                            text = stringResource(id = R.string.setting_keypress_beep),
                            checked = false,
                            onCheckedChange = {},
                            textStyle = textStyle,
                            isClickable = true
                        )
                        IKeyDivider()
                        SettingItemSwitch(
                            text = stringResource(id = R.string.setting_secure_mode),
                            checked = false,
                            onCheckedChange = {},
                            textStyle = textStyle,
                            isClickable = true
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

@RequiresApi(Build.VERSION_CODES.Q)
@Preview
@Composable
private fun PreviewDisconnected() {
    FuhsingSmartLockV2AndroidTheme {
        SettingsScreen(
            onNaviUpClick = {}, onDeleteClick = {}, onEditClick = {}, onAutoLockDone = {}, onEventLogClick = {}, onVacationModeClick = {},
            onAutoLockClick = {}, onKeypressBeepClick = {}, onSecureModeClick = {}, onChangeAdminCodeClick = {},
            onResetBoltDirectionClick = {}, onWiFiSettingClick = {}, onFactoryResetClick = {}, isConnected = false, state = SettingsUiState(), tempState = null)
    }
}