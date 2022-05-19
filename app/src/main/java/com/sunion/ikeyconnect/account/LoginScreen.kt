package com.sunion.ikeyconnect.account

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.IkeyAlertDialog
import com.sunion.ikeyconnect.ui.component.InputTextField
import com.sunion.ikeyconnect.ui.component.LoadingScreen
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: LoginViewModel, navController: NavController, toHome: () -> Unit) {
    //navController for go to create account or forget pw
    LoginScreen(
        onLoginClick = vm::login,
        email = vm.email.value/*?:""*/,
        password = vm.password.value/*?:""*/,
        onEmailChange = vm::setEmail,
        onPasswordChange = vm::setPassword,
        onForgotPasswordClick = { navController.navigate(AccountNavi.ForgotPassword.route) },
        onCreateAccountClick = { navController.navigate(AccountNavi.CreateAccountEmail.route) },
        emailError = vm.emailError.value,
        passwordError = vm.passwordError.value
    )

    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        vm.uiEvent.collect {
            when(it){
                LoginViewModel.UiEvent.Success -> {
                    vm.setPassword("")
                    toHome.invoke()
                }
                is LoginViewModel.UiEvent.Fail -> errorMessage = it.message
            }
        }
    }

    if (vm.loading.value)
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
fun LoginScreen(
    onLoginClick: () -> Unit,
    email: String,
    password: String,
    onEmailChange:(String) -> Unit,
    onPasswordChange:(String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    emailError: String,
    passwordError: String,
) {
    val mContext = LocalContext.current // for Toast
    val mActivity = LocalContext.current as Activity
    //If init awsMobileClient here will take too many sources to display the preview correctly.
    val idValue = remember { mutableStateOf(TextFieldValue()) }
    val passwordValue = remember { mutableStateOf(TextFieldValue()) }
    val openDialog = remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val (focusRequester) = remember { FocusRequester.createRefs() }
    BackHandler(onBack = { mActivity.finish() })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = dimensionResource(id = R.dimen.space_28))
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_34)))
        Image(
            painter = painterResource(id = R.drawable.logo_mark),
            contentDescription = "iKey",
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.space_86))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_37)))
        Text(
            text = stringResource(id = R.string.account_login),
            style = TextStyle(
                color = colorResource(id = R.color.black),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_47)))
        InputTextField(
            title = stringResource(id = R.string.account_email),
            value = email,
            onValueChange = onEmailChange,
            hint = "example@mail.com",
            error = emailError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                keyboardController?.hide()
                coroutineScope.launch {
                    delay(300)
                    focusRequester.requestFocus()
                }
            }),
            modifier = Modifier.testTag("email")
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_27)))

        InputTextField(
            title = stringResource(id = R.string.account_password),
            value = password,
            onValueChange = onPasswordChange,
            error = passwordError,
            protect = true,
            modifier = Modifier
                .focusRequester(focusRequester)
                .testTag("password"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_7)))
        Text(
            text = stringResource(id = R.string.account_forgot_password),
            style = TextStyle(color = colorResource(id = R.color.blue_info)),
            modifier = Modifier.clickable(onClick = onForgotPasswordClick)
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_27)))
        PrimaryButton(
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.account_log_in)
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_28)))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Line()
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_7)))
            Text(
                text = stringResource(id = R.string.account_or),
                style = TextStyle(
                    color = colorResource(id = R.color.black),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_7)))
            Line()
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_28)))
        CreateAccountButton(
            onCreateAccountClick = onCreateAccountClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

//    Column( modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Top) {
//        Row( horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
//            Button(
//                onClick = {
////                Toast.makeText(mContext, "${idValue.value.text} ${passwordValue.value.text}", Toast.LENGTH_SHORT).show()
//                    openDialog.value = true
//                }
//            ){ Text("Login") }
//        }
//    }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            title = {
                Text(text = "Enter id and pw")
            },
            text = {
                Column() {
                    TextField(
                        value = idValue.value,
                        onValueChange = {
                            idValue.value = it
                            onEmailChange.invoke(idValue.value.text)
                        },
                        placeholder = { Text(text = "Enter your id") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = true,
                            keyboardType = KeyboardType.Text
                        )
                    )
                    TextField(
                        value = passwordValue.value,
                        onValueChange = {
                            passwordValue.value = it
                            onPasswordChange.invoke(passwordValue.value.text)
                        },
                        placeholder = { Text(text = "Enter your password") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = true,
                            keyboardType = KeyboardType.Text
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLoginClick.invoke()
                        Toast.makeText(mContext, "$email $password", Toast.LENGTH_SHORT).show()
                        openDialog.value = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = { openDialog.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun Line() {
    Spacer(
        modifier = Modifier
            .height(1.dp)
            .width(dimensionResource(id = R.dimen.space_57))
            .background(colorResource(id = R.color.popup_text))
    )
}

@Composable
private fun CreateAccountButton(
    onCreateAccountClick: () -> Unit,
    modifier: Modifier
) {
    OutlinedButton(
        onClick = onCreateAccountClick,
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
        modifier = modifier
            .height(dimensionResource(id = R.dimen.space_41))
            .width(dimensionResource(id = R.dimen.space_248))
            .border(
                BorderStroke(1.dp, MaterialTheme.colors.primary),
                RoundedCornerShape(percent = 50)
            )
    ) {
        Text(
            text = stringResource(id = R.string.account_create_new_account),
            style = MaterialTheme.typography.colorPrimaryBoldSize18
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
    LoginScreen(
            email = "",
            onEmailChange = {},
            password = "",
            onPasswordChange = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onCreateAccountClick = {},
            emailError = "",
            passwordError = ""
        )
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun Preview2() {
    FuhsingSmartLockV2AndroidTheme {
    LoginScreen(
            email = "",
            onEmailChange = {},
            password = "12345678",
            onPasswordChange = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onCreateAccountClick = {},
            emailError = "User does not exist.",
            passwordError = ""
        )
    }
}