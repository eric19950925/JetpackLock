package com.sunion.ikeyconnect.account.createAccount

import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.sunion.ikeyconnect.account.PasswordTooltip
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18
import kotlinx.coroutines.flow.collect
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.account.AccountNavi
import com.sunion.ikeyconnect.ui.component.*

@Composable
fun CreateAccountPasswordScreen(viewModel: CreateAccountViewModel, navController: NavController) {
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            if (it is CreateAccountViewModel.UiEvent.GoValidateScreen)
                navController.navigate(AccountNavi.CreateAccountValidate.route)
            else if (it is CreateAccountViewModel.UiEvent.SignUpFail)
                errorMessage = it.message
        }
    }

    CreateAccountPasswordScreen(
        password = viewModel.password.value,
        onPasswordChange = viewModel::setPassword,
        passwordConfirm = viewModel.passwordConfirm.value,
        onPasswordConfirmChange = viewModel::setPasswordConfirm,
        onConfirmClick = viewModel::checkPassword,
        onNaviUpClick = { navController.popBackStack() },
        passwordError = viewModel.passwordError.value,
        passwordConfirmError = viewModel.passwordConfirmError.value,
        showPasswordTips = viewModel.showPasswordTips.value
    )

    if (viewModel.loading.value)
        LoadingScreen()

    if (errorMessage.isNotEmpty())
        IkeyAlertDialog(
            onDismissRequest = { errorMessage = "" },
            onConfirmButtonClick = { errorMessage = "" },
            title = "Error",
            text = errorMessage
        )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreateAccountPasswordScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordConfirm: String,
    onPasswordConfirmChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    passwordError: String = "",
    passwordConfirmError: String = "",
    onNaviUpClick: () -> Unit,
    showPasswordTips: Boolean
) {
    var passwordTooltipOffset by remember { mutableStateOf<Offset?>(null) }
    val (focusRequester) = FocusRequester.createRefs()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            IKeyTopAppBar(
                onNaviUpClick = onNaviUpClick,
                modifier = Modifier.background(Color.White).padding(horizontal = dimensionResource(id = R.dimen.space_20))
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .padding(horizontal = dimensionResource(id = R.dimen.space_28))
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_40)))
                Text(
                    text = stringResource(id = R.string.account_create_a_password),
                    style = MaterialTheme.typography.colorPrimaryBoldSize18
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_46)))
                InputTextField(
                    title = stringResource(id = R.string.account_new_password),
                    value = password,
                    onValueChange = onPasswordChange,
                    error = passwordError,
                    hint = stringResource(id = R.string.account_enter_your_new_password),
                    modifier = Modifier
                        .onGloballyPositioned { layoutCoordinates ->
                            passwordTooltipOffset = layoutCoordinates.boundsInParent().bottomLeft
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusRequester.requestFocus() }),
                    protect = true
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_27)))
                InputTextField(
                    title = stringResource(id = R.string.account_confirm_password),
                    value = passwordConfirm,
                    onValueChange = onPasswordConfirmChange,
                    error = passwordConfirmError,
                    hint = stringResource(id = R.string.account_confirm_the_password),
                    modifier = Modifier.focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    protect = true
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_48)))
                PrimaryButton(
                    text = stringResource(id = R.string.global_confirm),
                    onClick = onConfirmClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                if (showPasswordTips && passwordTooltipOffset != null) {
                    PasswordTooltip(passwordTooltipOffset!!)
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        CreateAccountPasswordScreen(
            password = "",
            onPasswordChange = {},
            passwordConfirm = "",
            onPasswordConfirmChange = {},
            onConfirmClick = { },
            onNaviUpClick = {},
            showPasswordTips = false
        )
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun Preview2() {
    FuhsingSmartLockV2AndroidTheme {
        CreateAccountPasswordScreen(
            password = "",
            onPasswordChange = {},
            passwordConfirm = "",
            onPasswordConfirmChange = {},
            onConfirmClick = { },
            onNaviUpClick = {},
            showPasswordTips = true
        )
    }
}