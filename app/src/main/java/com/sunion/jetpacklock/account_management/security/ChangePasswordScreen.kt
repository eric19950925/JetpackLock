package com.sunion.jetpacklock.account_management.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.sunion.jetpacklock.account.PasswordTooltip
import com.sunion.jetpacklock.ui.component.*
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.R

@Composable
fun ChangePasswordScreen(
    viewModel: SecurityViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ChangePasswordScreen(
        onNaviUpClick = { navController.popBackStack() },
        currentPassword = viewModel.currentPassword.value,
        onCurrentPasswordChange = viewModel::setCurrentPassword,
        newPassword = viewModel.newPassword.value,
        onNewPasswordChange = viewModel::setNewPassword,
        confirmNewPassword = viewModel.confirmNewPassword.value,
        onConfirmNewPasswordChange = viewModel::setConfirmNewPassword,
        onSaveClick = viewModel::changePasswordToRemote,
        modifier = modifier,
        newPasswordError = viewModel.newPasswordError.value,
        confirmNewPasswordError = viewModel.confirmNewPasswordError.value,
        showPasswordTips = viewModel.showPasswordTips.value
    )

    val saveSuccess = viewModel.saveSuccess.value
    if (saveSuccess != null)
        IkeyAlertDialog(
            onDismissRequest = viewModel::clearSaveResult,
            title = if (saveSuccess == true) "success" else "fail",
            text = "",
            onConfirmButtonClick = {
                if (saveSuccess == true)
                    navController.popBackStack()
                viewModel.clearSaveResult()
            })

    if (viewModel.isLoading.value)
        LoadingScreen()
}

@Composable
fun ChangePasswordScreen(
    onNaviUpClick: () -> Unit,
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmNewPassword: String,
    onConfirmNewPasswordChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    newPasswordError: String,
    confirmNewPasswordError: String,
    showPasswordTips: Boolean
) {
    var passwordTooltipOffset by remember { mutableStateOf<Offset?>(null) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            BackTopAppBar(
                onNaviUpClick = onNaviUpClick,
                title = stringResource(id = R.string.member_change_password)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(it)
                .padding(horizontal = dimensionResource(id = R.dimen.space_28))
        ) {
            IKeyDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_45)))
            InputTextField(
                title = stringResource(id = R.string.member_current_password),
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                hint = stringResource(id = R.string.member_enter_your_current_password),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                protect = true
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_27)))
            InputTextField(
                title = stringResource(id = R.string.account_new_password),
                value = newPassword,
                onValueChange = onNewPasswordChange,
                hint = stringResource(id = R.string.member_enter_your_new_password),
                error = newPasswordError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    passwordTooltipOffset = coordinates.boundsInParent().bottomLeft
                },
                protect = true
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_27)))
            InputTextField(
                title = stringResource(id = R.string.account_confirm_password),
                value = confirmNewPassword,
                onValueChange = onConfirmNewPasswordChange,
                hint = stringResource(id = R.string.member_confirm_your_new_password),
                error = confirmNewPasswordError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                protect = true
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(id = R.string.member_save),
                onClick = onSaveClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))

            if (showPasswordTips && passwordTooltipOffset != null) {
                PasswordTooltip(passwordTooltipOffset!!)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewChangePasswordScreen() {
    FuhsingSmartLockV2AndroidTheme {
        ChangePasswordScreen(
            onNaviUpClick = { },
            currentPassword = "",
            onCurrentPasswordChange = { },
            newPassword = "newPassword",
            onNewPasswordChange = { },
            confirmNewPassword = "confirmNewPassword",
            onConfirmNewPasswordChange = { },
            onSaveClick = { },
            newPasswordError = "newPasswordError",
            confirmNewPasswordError = "confirmNewPasswordError",
            showPasswordTips = false
        )
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun PreviewChangePasswordScreenSmall() {
    FuhsingSmartLockV2AndroidTheme {
        ChangePasswordScreen(
            onNaviUpClick = { },
            currentPassword = "",
            onCurrentPasswordChange = { },
            newPassword = "newPassword",
            onNewPasswordChange = { },
            confirmNewPassword = "confirmNewPassword",
            onConfirmNewPasswordChange = { },
            onSaveClick = { },
            newPasswordError = "newPasswordError",
            confirmNewPasswordError = "confirmNewPasswordError",
            showPasswordTips = true
        )
    }
}