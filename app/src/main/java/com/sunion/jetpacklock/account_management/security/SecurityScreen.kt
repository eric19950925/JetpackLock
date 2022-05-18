package com.sunion.jetpacklock.account_management.security

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.account_management.MemberManagementRoute
import com.sunion.jetpacklock.ui.component.*
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import kotlinx.coroutines.flow.collect

@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onSignOutSuccess: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is SecurityViewModel.UiEvent.SignOutAllFail ->
                    Toast.makeText(context, "SignOutAllFail", Toast.LENGTH_SHORT).show()
                SecurityViewModel.UiEvent.SignOutAllSuccess ->
                {
                    viewModel.logOut()
                    Toast.makeText(context, "SignOutAllSuccess", Toast.LENGTH_SHORT).show()
                    onSignOutSuccess.invoke()
                }
            }
        }
    }

    SecurityScreen(
        onNaviUpClick = { navController.popBackStack() },
        onLogOutAllDevicesClick = { viewModel.displayLogOutAllDeviceAlert() },
        modifier = modifier,
        onPasswordClick = {
            navController.navigate(MemberManagementRoute.ChangePassword.route)
        }
    )

    if (viewModel.showLogOutAllDeviceAlert.value)
        IkeyAlertDialog(
            onDismissRequest = viewModel::closeLogOutAllDeviceAlert,
            title = stringResource(id = R.string.member_log_out_of_all_devices),
            text = stringResource(id = R.string.member_log_out_of_all_devices_tips),
            onConfirmButtonClick = viewModel::logOutAllDevice,
            onDismissButtonClick = viewModel::closeLogOutAllDeviceAlert,
            dismissButtonText = stringResource(id = R.string.global_cancel)
        )
}

@Composable
fun SecurityScreen(
    onNaviUpClick: () -> Unit,
    onLogOutAllDevicesClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPasswordClick: () -> Unit
) {
    Scaffold(
        topBar = {
            BackTopAppBar(
                onNaviUpClick = onNaviUpClick,
                title = stringResource(id = R.string.member_security)
            )
        },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            IKeyDivider(modifier = Modifier.fillMaxWidth())
            TwoLineRow(
                title = stringResource(id = R.string.account_password),
                text = "●●●●●●●●",
                modifier = Modifier.clickable(onClick = onPasswordClick)
            )
            IKeyDivider(modifier = Modifier.fillMaxWidth())
            FunctionRow(
                onClick = onLogOutAllDevicesClick,
                text = stringResource(id = R.string.member_log_out_of_all_devices)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSecurityScreen() {
    FuhsingSmartLockV2AndroidTheme {
        SecurityScreen(
            onNaviUpClick = { },
            onLogOutAllDevicesClick = { }
        ) {}
    }
}