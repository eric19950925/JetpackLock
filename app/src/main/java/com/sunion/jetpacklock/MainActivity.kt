package com.sunion.jetpacklock

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
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
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.SignOutOptions
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.SignInResult

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleComposable()
        }
    }
}

@Composable
fun SimpleComposable() {
    val mContext = LocalContext.current // for Toast
    //If init awsMobileClient here will take too many sources to display the preview correctly.
    val awsMobileClient = AWSMobileClient.getInstance().apply {
        initialize(mContext, object : Callback<UserStateDetails> {
            override fun onResult(result: UserStateDetails?) {
                Log.d("TAG",result.toString())
            }

            override fun onError(e: Exception?) {
                Log.e("TAG",e.toString())
            }
        })
    }
    val idValue = remember { mutableStateOf(TextFieldValue()) }
    val passwordValue = remember { mutableStateOf(TextFieldValue()) }
    val openDialog = remember { mutableStateOf(false) }
    val openLogoutDialog = remember { mutableStateOf(false) }
    val logcat = remember { mutableStateOf("Welcome~\n\n") }

    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Button(
                onClick = {
//                Toast.makeText(mContext, "${idValue.value.text} ${passwordValue.value.text}", Toast.LENGTH_SHORT).show()
                    openDialog.value = true
                }
            ){
                Text("Login")
            }
            Button(
                onClick = {
                    openLogoutDialog.value = true
                }
            ){
                Text("Logout")
            }

            Button(
                onClick = {
                    logcat.value = ""
                }
            ){
                Text("Clean")
            }
        }
        Text(text = logcat.value)
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
                        onValueChange = { idValue.value = it },
                        placeholder = { Text(text = "Enter your id") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = true,
                            keyboardType = KeyboardType.Text
                        )
                    )
                    TextField(
                        value = passwordValue.value,
                        onValueChange = { passwordValue.value = it },
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
                        awsMobileClient.signIn(idValue.value.text, passwordValue.value.text, null, object: Callback<SignInResult>{
                            override fun onResult(result: SignInResult?) {
                                logcat.value = logcat.value + "login status: ${result?.signInState}\n"
                            }

                            override fun onError(e: java.lang.Exception?) {
                                logcat.value = logcat.value + "login error: $e\n"
                            }

                        })
                        Toast.makeText(mContext, "${idValue.value.text} ${passwordValue.value.text}", Toast.LENGTH_SHORT).show()
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
                        awsMobileClient.signOut(SignOutOptions.builder().build(), object :Callback<Void> {
                            override fun onResult(result: Void?) {
                                logcat.value = logcat.value + "logout success\n"
                            }

                            override fun onError(e: java.lang.Exception?) {
                                logcat.value = logcat.value + "logout error: $e\n"
                            }

                        })
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
    SimpleComposable()
}