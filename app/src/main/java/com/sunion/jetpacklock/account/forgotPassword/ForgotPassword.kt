package com.sunion.jetpacklock.account.forgotPassword

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.sunion.jetpacklock.account.AccountNavi
import com.sunion.jetpacklock.ui.component.InputTextField
import com.sunion.jetpacklock.ui.component.PrimaryButton
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.ui.component.BackTopAppBar
import com.sunion.jetpacklock.ui.theme.colorPrimaryBoldSize18
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.ui.component.LoadingScreen
import kotlinx.coroutines.flow.collect //Manual editing

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    navController: NavController
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            if (it is ForgotPasswordViewModel.UiEvent.Success)
                Log.d("TAG","to set new pw")

                navController.navigate("${AccountNavi.ForgotPasswordConfirm.route}/${viewModel.email.value}")
        }
    }
    ForgotPasswordScreen(
        onNaviUpClick = { navController.popBackStack() },
        email = viewModel.email.value,
        onEmailChange = viewModel::setEmail,
        onNextClick = viewModel::requestForgotPassword,
        emailError = viewModel.emailError.value
    )
    if (viewModel.loading.value)
        LoadingScreen()
}

@Composable
fun ForgotPasswordScreen(
    onNaviUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    email: String,
    onEmailChange: (String) -> Unit,
    onNextClick: () -> Unit,
    emailError: String
) {
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
                    title = stringResource(id = R.string.account_email),
                    subtitle = stringResource(id = R.string.account_please_enter_your_email),
                    value = email,
                    onValueChange = onEmailChange,
                    error = emailError,
                    hint = "example@mail.com"
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_48)))
                PrimaryButton(
                    text = stringResource(id = R.string.account_next),
                    onClick = onNextClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewForgotPasswordScreen() {
    FuhsingSmartLockV2AndroidTheme {
        ForgotPasswordScreen(
            onNaviUpClick = {},
            email = "",
            onEmailChange = {},
            onNextClick = {},
            emailError = ""
        )
    }
}