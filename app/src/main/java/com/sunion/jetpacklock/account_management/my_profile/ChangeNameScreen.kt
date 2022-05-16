package com.sunion.jetpacklock.account_management.my_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.sunion.jetpacklock.ui.component.*
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.ui.theme.colorPrimaryBoldSize18
import com.sunion.jetpacklock.R

@Composable
fun ChangeNameScreen(
    viewModel: MyProfileViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    ChangeNameScreen(
        onNaviUpClick = { navController.popBackStack() },
        name = viewModel.username.value,
        onNameChange = viewModel::setUsername,
        onSaveClick = viewModel::saveName,
        modifier = modifier
    )

    val saveSuccess = viewModel.saveSuccess.value
    if (saveSuccess != null)
        IkeyAlertDialog(
            onDismissRequest = viewModel::clearSaveResult,
            title = if (saveSuccess == true) "success" else "fail",
            text = "",
            onConfirmButtonClick = {
                if (saveSuccess == true)
                    navController.popBackStack()
                viewModel.clearSaveResult()
            })

    if (viewModel.isLoading.value)
        LoadingScreen()
}

@Composable
fun ChangeNameScreen(
    onNaviUpClick: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Scaffold(
        topBar = {
            BackTopAppBar(
                onNaviUpClick = onNaviUpClick,
                title = stringResource(id = R.string.member_change_name)
            )
        },
    ) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            IKeyDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_48)))

            Column(Modifier.padding(horizontal = dimensionResource(id = R.dimen.space_28))) {
                Text(
                    text = stringResource(id = R.string.member_change_name),
                    style = MaterialTheme.typography.colorPrimaryBoldSize18
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_14)))
                IKeyTextField(
                    value = name,
                    onValueChange = onNameChange,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(id = R.string.member_save),
                onClick = onSaveClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_20)))
        }
    }
}

@Preview
@Composable
private fun PreviewChangeNameScreen() {
    FuhsingSmartLockV2AndroidTheme {
        ChangeNameScreen(
            onNaviUpClick = { },
            name = "Loki Laufeyson",
            onNameChange = { },
            onSaveClick = { }
        )
    }
}