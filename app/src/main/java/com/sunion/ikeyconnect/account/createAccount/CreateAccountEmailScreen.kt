package com.sunion.ikeyconnect.account.createAccount

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import kotlinx.coroutines.flow.collect
import com.sunion.ikeyconnect.account.AccountNavi
import com.sunion.ikeyconnect.ui.component.IKeyTopAppBar
import com.sunion.ikeyconnect.ui.component.InputTextField
import com.sunion.ikeyconnect.ui.component.LoadingScreen
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18
import com.sunion.ikeyconnect.R

@Composable
fun CreateAccountEmailScreen(viewModel: CreateAccountViewModel, navController: NavController) {
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            if (it is CreateAccountViewModel.UiEvent.GoPasswordScreen)
                navController.navigate(AccountNavi.CreateAccountPassword.route)
        }
    }

    CreateAccountEmailScreen(
        email = viewModel.email.value,
        onEmailChange = viewModel::setEmail,
        onNextClick = viewModel::checkEmail,
        emailError = viewModel.emailError.value,
        onNaviUpClick = { navController.popBackStack() }
    )

    if (viewModel.loading.value)
        LoadingScreen()
}

@Composable
fun CreateAccountEmailScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    emailError: String = "",
    onNaviUpClick: () -> Unit
) {
    Scaffold(
        topBar = {
            IKeyTopAppBar(
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
                    text = stringResource(id = R.string.account_create_your_account),
                    style = MaterialTheme.typography.colorPrimaryBoldSize18
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_46)))
                InputTextField(
                    title = stringResource(id = R.string.account_your_email_address),
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

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_14)))
                val annotatedText = termAnnotatedString()
                ClickableText(
                    text = annotatedText,
                    style = TextStyle(
                        color = colorResource(id = R.color.grayACBECA),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "TERM", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                Log.d("TAG",annotation.item)
                            }
                        annotatedText.getStringAnnotations(
                            tag = "PRIVACY",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let { annotation ->
                                Log.d("TAG",annotation.item)
                            }
                    }
                )
            }
        }
    }
}

@Composable
private fun termAnnotatedString() = buildAnnotatedString {
    append(stringResource(id = R.string.account_terms_conditions_1))
    val style = SpanStyle(
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Medium
    )

    pushStringAnnotation(
        tag = "TERM",
        annotation = "https://developer.android.com"//TODO
    )
    withStyle(style) {
        append(" " + stringResource(id = R.string.account_terms_conditions))
    }
    pop()

    append(" " + stringResource(id = R.string.account_terms_conditions_2))

    pushStringAnnotation(
        tag = "PRIVACY",
        annotation = "https://developer.android.com"//TODO
    )
    withStyle(style) {
        append(" " + stringResource(id = R.string.account_privacy_policy))
    }
    pop()
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        CreateAccountEmailScreen(
            email = "",
            onEmailChange = {},
            onNextClick = {}
        ) {}
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun Preview2() {
    FuhsingSmartLockV2AndroidTheme {
        CreateAccountEmailScreen(
            email = "",
            onEmailChange = {},
            onNextClick = {},
            emailError = "This field is required."
        ) {}
    }
}