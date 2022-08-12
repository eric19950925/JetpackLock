package com.sunion.ikeyconnect.add_lock.admin_code

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.add_lock.component.AddLockScreenScaffold
import com.sunion.ikeyconnect.add_lock.component.EnableBluetoothDialog
import com.sunion.ikeyconnect.add_lock.component.ExitPromptDialog
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog
import com.sunion.ikeyconnect.ui.component.InputTextField
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import timber.log.Timber
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.ui.component.LoadingScreenDialog

@Composable
fun AdminCodeScreen(viewModel: AdminCodeViewModel, navController: NavController) {
    val bluetoothEnableLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            viewModel.checkIsBluetoothEnable()
        }

    BackHandler {
        viewModel.closeExitPromptDialog()
    }

    var showDisconnectionDialog by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                AdminCodeUiEvent.DeleteLockFail -> Timber.e("DeleteLockFail")
                AdminCodeUiEvent.DeleteLockSuccess ->
                    navController.popBackStack(HomeRoute.Home.route, false)
                AdminCodeUiEvent.Failed ->
                    if (!viewModel.isLockConnected()) showDisconnectionDialog = true
                AdminCodeUiEvent.Success ->
                    navController.navigate("${AddLockRoute.RequestLocation.route}/${viewModel.macAddress}/${viewModel.deviceType}")
            }
        }
    }

    val uiState = viewModel.uiState.collectAsState().value
    AdminCodeScreen(
        state = uiState,
        onNaviUpClick = viewModel::showExitPromptDialog,
        onLockNameChange = viewModel::setLockName,
        onNextClick = viewModel::execute,
        onAdminCodeChange = viewModel::setAdminCode,
        onTimezoneClick = viewModel::showTimeZoneMenu,
        onTimezoneItemClick = viewModel::setTimezone,
        onDismissTimeZoneMenu = viewModel::closeTimeZoneMenu,
        onUserNameChange = viewModel::setUsername
    )

    if (uiState.shouldShowExitDialog)
        ExitPromptDialog(
            onDismissRequest = viewModel::closeExitPromptDialog,
            onConfirmButtonClick = {
                viewModel.deleteLock()
                viewModel.closeExitPromptDialog()
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

    if (showDisconnectionDialog)
        IkeyAlertDialog(
            onDismissRequest = { },
            onConfirmButtonClick = { navController.popBackStack(HomeRoute.Home.route, false) },
            title = stringResource(id = R.string.global_disconnect),
            text = "",
            confirmButtonText = stringResource(id = R.string.global_confirm)
        )

    if (uiState.errorMessage.isNotEmpty())
        IkeyAlertDialog(
            onDismissRequest = { },
            onConfirmButtonClick = viewModel::closeErrorDialog,
            title = "Error",
            text = uiState.errorMessage,
            confirmButtonText = stringResource(id = R.string.global_confirm)
        )
    if (uiState.isProcessing)
        LoadingScreenDialog("")
}

@Composable
fun AdminCodeScreen(
    state: AdminCodeUiState,
    onNaviUpClick: () -> Unit,
    onLockNameChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onAdminCodeChange: (String) -> Unit,
    onTimezoneClick: () -> Unit,
    onTimezoneItemClick: (String) -> Unit,
    onDismissTimeZoneMenu: () -> Unit,
    onUserNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    AddLockScreenScaffold(
        onNaviUpClick = onNaviUpClick,
        title = stringResource(id = R.string.toolbar_title_add_lock),
        step = 4,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_40)))
        InputTextField(
            title = stringResource(id = R.string.global_lock_name),
            value = state.lockName,
            onValueChange = {
                if (it.length <= 20)
                    onLockNameChange(it)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28)),
        )
        if(!state.isWiFiLock) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_44)))
            InputTextField(
                title = stringResource(id = R.string.global_username),
                value = state.userName,
                onValueChange = {
                    if (it.length <= 20)
                        onUserNameChange(it)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28))
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_44)))
        var error by remember { mutableStateOf("") }
        val context = LocalContext.current
        InputTextField(
            title = stringResource(id = R.string.global_admin_code),
            value = if (state.hasAdminCodeBeenSet) stringResource(id = R.string.add_lock_admin_code_setup_already)
            else state.adminCode,
            onValueChange = {
                error = ""
                if (!state.hasAdminCodeBeenSet) {
                    if (it.length <= 6)
                        onAdminCodeChange(it)

                    if (it.length < 4)
                        error = context.getString(R.string.global_pin_code_length)
                }
            },
            error = error,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28))
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_44)))
        Box(modifier = modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28))) {
            InputTextField(
                title = stringResource(id = R.string.add_lock_timezone),
                value = state.timezone,
                onValueChange = onUserNameChange
            )
            Spacer(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clickable(onClick = onTimezoneClick)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_indicator_right),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .padding(bottom = 2.dp, end = 10.dp)
                    .rotate(90f)
                    .align(Alignment.BottomEnd)
            )
            DropdownMenu(
                expanded = state.showTimezoneMenu,
                onDismissRequest = onDismissTimeZoneMenu,
                modifier = Modifier.height(dimensionResource(id = R.dimen.space_400))
            ) {
                state.timezoneList.forEach {
                    DropdownMenuItem(onClick = { onTimezoneItemClick(it) }) {
                        Text(text = it)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(id = R.string.global_next),
            onClick = onNextClick,
            enabled = state.isNextEnable
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
    }
}

@Preview
@Composable
private fun PreviewAdminCodeScreen() {
    FuhsingSmartLockV2AndroidTheme {
        AdminCodeScreen(
            state = AdminCodeUiState(),
            onNaviUpClick = {},
            onLockNameChange = {},
            onNextClick = {},
            onAdminCodeChange = {},
            onTimezoneClick = {},
            onTimezoneItemClick = {},
            onDismissTimeZoneMenu = {},
            onUserNameChange = {},
        )
    }
}

@Preview
@Composable
private fun PreviewAdminCodeScreen2() {
    FuhsingSmartLockV2AndroidTheme {
        AdminCodeScreen(
            state = AdminCodeUiState(
                lockName = "New Lock",
                adminCode = "12345",
                timezone = "GMT + 0",
                isNextEnable = true,
                showTimezoneMenu = true,
                isWiFiLock = true
            ),
            onNaviUpClick = {},
            onLockNameChange = {},
            onNextClick = {},
            onAdminCodeChange = {},
            onTimezoneClick = {},
            onTimezoneItemClick = {},
            onDismissTimeZoneMenu = {},
            onUserNameChange = {}
        )
    }
}

@Preview
@Composable
private fun PreviewHasAdminCodeBeenSet() {
    FuhsingSmartLockV2AndroidTheme {
        AdminCodeScreen(
            state = AdminCodeUiState(
                lockName = "New Lock",
                userName = "userName",
                adminCode = "12345",
                timezone = "GMT + 0",
                isNextEnable = true,
                hasAdminCodeBeenSet = true
            ),
            onNaviUpClick = {},
            onLockNameChange = {},
            onNextClick = {},
            onAdminCodeChange = {},
            onTimezoneClick = {},
            onTimezoneItemClick = {},
            onDismissTimeZoneMenu = {},
            onUserNameChange = {}
        )
    }
}