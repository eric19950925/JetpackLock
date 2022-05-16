package com.sunion.jetpacklock.account_management

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.ui.theme.colorPrimaryBoldSize18
import com.sunion.jetpacklock.ui.theme.colorPrimaryMediumSize16
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.ui.component.*
import kotlinx.coroutines.flow.collect

@Composable
fun AccountScreen(
    viewModel: MemberManagementViewModel,
    navController: NavController,
    onNaviUpClick: () -> Unit,
    onSignOutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                MemberManagementViewModel.UiEvent.SignOutSuccess -> onSignOutSuccess()
                MemberManagementViewModel.UiEvent.DeleteAccountSuccess -> onSignOutSuccess()//todo
            }
        }
    }

    AccountScreen(
        username = viewModel.username.value,
        onMyProfileClick = { navController.navigate(MemberManagementRoute.MyProfile.route) },
        onSecurityClick = { navController.navigate(MemberManagementRoute.Security.route) },
        onSmartHomeClick = { },
        onNaviUpClick = onNaviUpClick,
        onLogoutClick = viewModel::logOut,
        onDeleteAccountClick = { viewModel.displayDeleteAccountAlert() },
        modifier = modifier
    )

    if (viewModel.showDeleteAccountAlert.value)
        Dialog(onDismissRequest = { viewModel.closeDeleteAccountAlert() }) {
            DeleteAccountAlertContent(
                deleteConfirmText = viewModel.deleteAccountConfirmText.value,
                onDeleteConfirmTextChange = viewModel::setDeleteAccountConfirmText,
                onDeleteClick = viewModel::deleteAccount,
                onCancelClick = { viewModel.closeDeleteAccountAlert() }
            )
        }

    if (viewModel.alertMessage.value.isNotEmpty())
        IkeyAlertDialog(
            onDismissRequest = { viewModel.clearAlertMessage() },
            onConfirmButtonClick = { viewModel.clearAlertMessage() },
            title = viewModel.alertMessage.value,
            text = ""
        )

    if (viewModel.isLoading.value)
        LoadingScreenDialog()
}

@Composable
fun AccountScreen(
    username: String,
    onMyProfileClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onSmartHomeClick: () -> Unit,
    onNaviUpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = {
        BackTopAppBar(
            onNaviUpClick = onNaviUpClick,
            title = stringResource(id = R.string.member_account)
        )
    }, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(it)
        ) {
            IKeyDivider(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.space_28)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = "pic",
                    modifier = Modifier.size(dimensionResource(id = R.dimen.space_63))
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_24)))
                Text(text = username, style = MaterialTheme.typography.colorPrimaryBoldSize18)
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_23)))
            IKeyDivider(modifier = Modifier.fillMaxWidth())
            AccountRow(stringResource(id = R.string.member_my_profile), onMyProfileClick)
            IKeyDivider(modifier = Modifier.fillMaxWidth())

            IKeyDivider(modifier = Modifier.fillMaxWidth())
            AccountRow(stringResource(id = R.string.member_security), onSecurityClick)
            IKeyDivider(modifier = Modifier.fillMaxWidth())

            IKeyDivider(modifier = Modifier.fillMaxWidth())
            AccountRow(stringResource(id = R.string.member_smart_home), onSmartHomeClick)
            IKeyDivider(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.weight(1f))

            FunctionRow(
                onClick = onLogoutClick,
                text = stringResource(id = R.string.member_logout)
            )
            IKeyDivider(modifier = Modifier.fillMaxWidth())
            FunctionRow(
                onClick = onDeleteAccountClick,
                text = stringResource(id = R.string.member_delete_account),
                textStyle = MaterialTheme.typography.colorPrimaryMediumSize16.copy(colorResource(id = R.color.redE60a17))
            )
        }
    }
}

@Composable
private fun AccountRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(id = R.dimen.space_28),
                vertical = dimensionResource(id = R.dimen.space_21)
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.colorPrimaryMediumSize16
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = "next",
            modifier = Modifier
                .size(18.dp)
                .rotate(180f)
        )
    }
}

@Preview
@Composable
private fun PreviewAccountScreen() {
    FuhsingSmartLockV2AndroidTheme {
        AccountScreen(
            username = "Username",
            onMyProfileClick = { },
            onSecurityClick = { },
            onSmartHomeClick = { },
            onNaviUpClick = { },
            onLogoutClick = { },
            onDeleteAccountClick = { })
    }
}

@Composable
fun DeleteAccountAlertContent(
    deleteConfirmText: String,
    onDeleteConfirmTextChange: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(dimensionResource(id = R.dimen.space_270))
            .background(Color.White, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp)
        ) {
            Text(
                text = stringResource(id = R.string.member_delete_account),
                style = TextStyle(
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_2)))
            Text(
                text = stringResource(id = R.string.member_delete_account_tips),
                style = TextStyle(
                    color = Color.Black,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))
            Text(
                text = stringResource(id = R.string.member_please_enter_password),
                style = TextStyle(
                    color = Color(0xffff0000),
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_10)))
            IKeyTextField(
                value = deleteConfirmText,
                onValueChange = onDeleteConfirmTextChange
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_16)))
        IKeyDivider(Modifier.fillMaxWidth())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onCancelClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.global_cancel),
                    style = TextStyle(
                        color = colorResource(id = R.color.blue_info),
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
            IKeyDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.global_delete),
                    style = TextStyle(
                        color = Color(0xffff3b30),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewDeleteAccountAlertContent() {
    DeleteAccountAlertContent(
        deleteConfirmText = "",
        onDeleteConfirmTextChange = {},
        onDeleteClick = {},
        onCancelClick = {})
}