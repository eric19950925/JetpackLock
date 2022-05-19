package com.sunion.ikeyconnect.account.forgotPassword

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.ui.component.BackTopAppBar
import com.sunion.ikeyconnect.ui.component.InputTextField
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.account.AccountNavi
import com.sunion.ikeyconnect.account.PasswordTooltip
import com.sunion.ikeyconnect.ui.component.LoadingScreen
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18
import kotlinx.coroutines.flow.collect //Manual editing


@Composable
fun ForgotPasswordConfirmScreen(
    viewModel: ForgotPasswordConfirmViewModel,
    navController: NavController
) {

    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            if (it is ForgotPasswordConfirmViewModel.UiEvent.Success)
                navController.navigate(AccountNavi.ForgotPasswordSuccess.route)
        }
    }

    ForgotPasswordConfirmScreen(
        onNaviUpClick = { navController.popBackStack() },
        confirmCode = viewModel.confirmCode.value,
        onConfirmCodeChange = viewModel::setConfirmCode,
        onConfirmClick = viewModel::confirmForgotPassword,
        confirmCodeError = viewModel.confirmCodeError.value,
        newPassword = viewModel.newPassword.value,
        onNewPasswordChange = viewModel::setNewPassword,
        newPasswordError = viewModel.newPasswordError.value,
        confirmPassword = viewModel.confirmPassword.value,
        onConfirmPasswordChange = viewModel::setConfirmPassword,
        confirmPasswordError = viewModel.confirmPasswordError.value,
        onResendClick = viewModel::resendConfirmCode,
        showPasswordTips = viewModel.showPasswordTips.value
    )

    if (viewModel.loading.value)
        LoadingScreen()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgotPasswordConfirmScreen(
    onNaviUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    confirmCode: String,
    onConfirmCodeChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    confirmCodeError: String,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    newPasswordError: String,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordError: String,
    onResendClick: () -> Unit,
    showPasswordTips: Boolean
) {
    var passwordTooltipOffset by remember { mutableStateOf<Offset?>(null) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            BackTopAppBar(
                onNaviUpClick = onNaviUpClick,
                modifier = Modifier.background(Color.White).padding(horizontal = dimensionResource(id = R.dimen.space_20))
            )
        },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .padding(horizontal = dimensionResource(id = R.dimen.space_28))
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_40)))
                Text(
                    text = stringResource(id = R.string.account_forgot_password_title),
                    style = MaterialTheme.typography.colorPrimaryBoldSize18
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_46)))
                InputTextField(
                    title = stringResource(id = R.string.account_verification_code),
                    subtitle = stringResource(id = R.string.account_verification_code_has_been_sent),
                    value = confirmCode,
                    onValueChange = onConfirmCodeChange,
                    error = confirmCodeError,
                    protect = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_12)))
                Text(
                    text = stringResource(id = R.string.account_resend),
                    style = TextStyle(
                        color = colorResource(id = R.color.blue_info),
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.clickable(onClick = onResendClick)
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_19)))
                InputTextField(
                    title = stringResource(id = R.string.account_new_password),
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    error = newPasswordError,
                    hint = stringResource(id = R.string.account_enter_your_new_password),
                    protect = true,
                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                        passwordTooltipOffset =
                            layoutCoordinates.boundsInParent().bottomLeft.copy(
                                x = 100f,
                                y = layoutCoordinates.boundsInParent().bottomLeft.y + 180f
                            )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_27)))
                InputTextField(
                    title = stringResource(id = R.string.account_confirm_password),
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    error = confirmPasswordError,
                    hint = stringResource(id = R.string.account_confirm_the_password),
                    protect = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_48)))
                PrimaryButton(
                    text = stringResource(id = R.string.global_confirm),
                    onClick = onConfirmClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }


    if (showPasswordTips && passwordTooltipOffset != null) {
        PasswordTooltip(passwordTooltipOffset!!)
    }
}

@Preview
@Composable
private fun PreviewNormal() {
    FuhsingSmartLockV2AndroidTheme {
        ForgotPasswordConfirmScreen(
            onNaviUpClick = {},
            confirmCode = "",
            onConfirmCodeChange = {},
            onConfirmClick = {},
            confirmCodeError = stringResource(id = R.string.account_incorrect_verification_code),
            newPassword = "",
            onNewPasswordChange = {},
            newPasswordError = "",
            confirmPassword = "",
            onConfirmPasswordChange = {},
            confirmPasswordError = stringResource(id = R.string.account_password_does_not_match),
            onResendClick = {},
            showPasswordTips = false
        )
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun PreviewSmallScreen() {
    FuhsingSmartLockV2AndroidTheme {
        ForgotPasswordConfirmScreen(
            onNaviUpClick = {},
            confirmCode = "",
            onConfirmCodeChange = {},
            onConfirmClick = {},
            confirmCodeError = stringResource(id = R.string.account_incorrect_verification_code),
            newPassword = "",
            onNewPasswordChange = {},
            newPasswordError = "",
            confirmPassword = "",
            onConfirmPasswordChange = {},
            confirmPasswordError = stringResource(id = R.string.account_password_does_not_match),
            onResendClick = {},
            showPasswordTips = true
        )
    }
}