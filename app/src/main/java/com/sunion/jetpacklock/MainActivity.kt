package com.sunion.jetpacklock

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: LoginViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleComposable(vm)
        }
    }
}

@Composable
fun SimpleComposable(vm: LoginViewModel) {
    SimpleComposable(
        onLoginClick = vm::login,
        email = vm.email.value?:"",
        password = vm.password.value?:"",
        onEmailChange = vm::setEmail,
        onPasswordChange = vm::setPassword,
        logger = vm.logger.value?:"Welcome~\n",
        onLoggerClean = vm::cleanLogger,
        onLogOutClick = vm::logOut
    )
}
@Composable
fun SimpleComposable(
    onLoginClick: () -> Unit,
    email: String,
    password: String,
    onEmailChange:(String) -> Unit,
    onPasswordChange:(String) -> Unit,
    logger: String,
    onLoggerClean: () -> Unit,
    onLogOutClick:() -> Unit
) {
    val mContext = LocalContext.current // for Toast
    //If init awsMobileClient here will take too many sources to display the preview correctly.
    val idValue = remember { mutableStateOf(TextFieldValue()) }
    val passwordValue = remember { mutableStateOf(TextFieldValue()) }
    val openDialog = remember { mutableStateOf(false) }
    val openLogoutDialog = remember { mutableStateOf(false) }
    val logcat = remember { mutableStateOf("Welcome~\n\n") }

    Column( modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Top) {
        Row( horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Button(
                onClick = {
//                Toast.makeText(mContext, "${idValue.value.text} ${passwordValue.value.text}", Toast.LENGTH_SHORT).show()
                    openDialog.value = true
                }
            ){ Text("Login") }
            Button(
                onClick = {
                    openLogoutDialog.value = true
                }
            ){ Text("Logout") }

            Button(
                onClick = onLoggerClean
            ){ Text("Clean") }
        }
        Text(text = logger)
    }

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
    if (openLogoutDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openLogoutDialog.value = false
            },
            title = {
                Text(text = "Are you sure to log out?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        //SignOutOptions can not be null
                        onLogOutClick.invoke()
                        openLogoutDialog.value = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = { openLogoutDialog.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
@Preview(showSystemUi = true)
fun MainActivityPreview(){
    SimpleComposable(
        onLoginClick = {},
        email = "",
        onEmailChange = {},
        password = "12345678",
        onPasswordChange = {},
        logger = "Welcome!!",
        onLoggerClean = {},
        onLogOutClick = {}
    )
}