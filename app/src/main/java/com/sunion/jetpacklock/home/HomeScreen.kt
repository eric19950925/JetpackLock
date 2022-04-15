package com.sunion.jetpacklock.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.account.LoginViewModel

@Composable
fun HomeScreen(onLogOutClick:() -> Unit) {
    val openLogoutDialog = remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = "logo",
                modifier = Modifier.size(90.dp)
            )
            Spacer(modifier = Modifier.height(55.dp))
            Button(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                onClick = {
                    openLogoutDialog.value = true
                }
            ){ Text("Logout") }
        }
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

@Preview(showSystemUi = true)
@Composable
private fun Preview() {
    HomeScreen(onLogOutClick = {})
}

