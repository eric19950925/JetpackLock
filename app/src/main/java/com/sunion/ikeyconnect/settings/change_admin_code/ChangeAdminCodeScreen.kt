package com.sunion.ikeyconnect.settings.change_admin_code

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
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.account.PasswordTooltip
import com.sunion.ikeyconnect.ui.component.*
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun ChangeAdminCodeScreen(
    viewModel: ChangeAdminCodeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ChangeAdminCodeScreen(
        onNaviUpClick = { navController.popBackStack() },
        currentPassword = viewModel.currentPassword.value,
        onCurrentPasswordChange = viewModel::setCurrentPassword,
        newPassword = viewModel.newPassword.value,
        onNewPasswordChange = viewModel::setNewPassword,
        confirmNewPassword = viewModel.confirmNewPassword.value,
        onConfirmNewPasswordChange = viewModel::setConfirmNewPassword,
        onSaveClick = viewModel::onSaveClick,
        modifier = modifier,
        oldPasswordError = viewModel.currentPasswordError.value,
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
fun ChangeAdminCodeScreen(
    onNaviUpClick: () -> Unit,
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmNewPassword: String,
    onConfirmNewPasswordChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    oldPasswordError: String,
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
                title = stringResource(id = R.string.setting_admin_code)
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
                title = stringResource(id = R.string.change_admin_code_old),
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                error = oldPasswordError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                protect = true
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_27)))
            InputTextField(
                title = stringResource(id = R.string.change_admin_code_new),
                value = newPassword,
                onValueChange = onNewPasswordChange,
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
                title = stringResource(id = R.string.change_admin_code_cofirm),
                value = confirmNewPassword,
                onValueChange = onConfirmNewPasswordChange,
//                hint = stringResource(id = R.string.member_confirm_your_new_password),
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
        ChangeAdminCodeScreen(
            onNaviUpClick = { },
            currentPassword = "",
            onCurrentPasswordChange = { },
            newPassword = "newPassword",
            onNewPasswordChange = { },
            confirmNewPassword = "confirmNewPassword",
            onConfirmNewPasswordChange = { },
            onSaveClick = { },
            oldPasswordError = "oldPasswordError",
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
        ChangeAdminCodeScreen(
            onNaviUpClick = { },
            currentPassword = "",
            onCurrentPasswordChange = { },
            newPassword = "newPassword",
            onNewPasswordChange = { },
            confirmNewPassword = "confirmNewPassword",
            onConfirmNewPasswordChange = { },
            onSaveClick = { },
            oldPasswordError = "oldPasswordError",
            newPasswordError = "newPasswordError",
            confirmNewPasswordError = "confirmNewPasswordError",
            showPasswordTips = true
        )
    }
}