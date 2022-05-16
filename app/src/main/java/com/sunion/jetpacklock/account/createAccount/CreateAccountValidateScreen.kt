package com.sunion.jetpacklock.account.createAccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.ui.theme.colorPrimaryBoldSize18
import com.sunion.jetpacklock.ui.theme.colorPrimaryRegularSize12
import kotlinx.coroutines.flow.collect
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.account.AccountNavi
import com.sunion.jetpacklock.ui.component.*

@Composable
fun CreateAccountValidateScreen(viewModel: CreateAccountViewModel, navController: NavController) {
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            if (it is CreateAccountViewModel.UiEvent.GoSuccessScreen)
                navController.navigate(AccountNavi.CreateAccountSuccess.route)
            else if (it is CreateAccountViewModel.UiEvent.ResendSignUpConfirmCodeFail) {
                errorMessage = it.message
            }
        }
    }
    CreateAccountValidateScreen(
        code = viewModel.signUpConfirmCode.value,
        onCodeChange = viewModel::setVerificationCode,
        onConfirmClick = viewModel::checkVerificationCode,
        onResendClick = viewModel::resendVerificationCode
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

@Composable
fun CreateAccountValidateScreen(
    code: String,
    onCodeChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onResendClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            IKeyTopAppBar(
                {},
                modifier = Modifier.background(Color.White).padding(horizontal = dimensionResource(id = R.dimen.space_20))
            )
        }
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
                Text(
                    text = stringResource(id = R.string.account_verification_code),
                    style = MaterialTheme.typography.colorPrimaryBoldSize18
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))
                Text(
                    text = stringResource(id = R.string.account_verification_code_has_been_sent),
                    style = MaterialTheme.typography.colorPrimaryRegularSize12
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_13)))
                IKeyTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    protected = true
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

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_118)))
                PrimaryButton(
                    text = stringResource(id = R.string.global_confirm),
                    onClick = onConfirmClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        CreateAccountValidateScreen(
            code = "",
            onCodeChange = { },
            onConfirmClick = { },
            onResendClick = { }
        )
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun Preview2() {
    FuhsingSmartLockV2AndroidTheme {
        CreateAccountValidateScreen(
            code = "",
            onCodeChange = { },
            onConfirmClick = { },
            onResendClick = { }
        )
    }
}