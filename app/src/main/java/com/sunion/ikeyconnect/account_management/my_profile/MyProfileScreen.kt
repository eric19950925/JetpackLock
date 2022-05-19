package com.sunion.ikeyconnect.account_management

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.sunion.ikeyconnect.ui.component.BackTopAppBar
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.account_management.my_profile.MyProfileViewModel
import com.sunion.ikeyconnect.ui.component.LoadingScreen
import com.sunion.ikeyconnect.ui.component.TwoLineRow
import com.sunion.ikeyconnect.R

@Composable
fun MyProfileScreen(
    viewModel: MyProfileViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    MyProfileScreen(
        onNaviUpClick = { navController.popBackStack() },
        name = viewModel.username.value,
        email = viewModel.email.value,
        onNameClick = { navController.navigate(MemberManagementRoute.ChangeName.route) },
        modifier = modifier
    )

    if (viewModel.isLoading.value)
        LoadingScreen()
}

@Composable
fun MyProfileScreen(
    onNaviUpClick: () -> Unit,
    name: String,
    email: String,
    onNameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            BackTopAppBar(
                onNaviUpClick = onNaviUpClick,
                title = stringResource(id = R.string.member_my_profile)
            )
        },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(it)
            ) {
                IKeyDivider(modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(id = R.dimen.space_28)),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "pic",
                        modifier = Modifier.size(dimensionResource(id = R.dimen.space_90))
                    )
                }
                IKeyDivider(modifier = Modifier.fillMaxWidth())
                TwoLineRow(
                    title = stringResource(id = R.string.member_name),
                    text = name,
                    modifier = Modifier.clickable(onClick = onNameClick)
                )
                IKeyDivider(modifier = Modifier.fillMaxWidth())
                TwoLineRow(
                    title = stringResource(id = R.string.account_email),
                    text = email,
                    showArrow = false
                )
                IKeyDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMyProfileScreen() {
    FuhsingSmartLockV2AndroidTheme {
        MyProfileScreen(
            onNaviUpClick = { },
            name = "Loki Laufeyson",
            email = "aabbcc@mail.com",
            onNameClick = {}
        )
    }
}