package com.sunion.ikeyconnect.users

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.BackTopAppBar
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun UsersScreen(viewModel: UsersViewModel, navController: NavController) {
    val uiState = viewModel.uiState.collectAsState().value
//    val tempUiState = viewModel.newRegistryAttributes.collectAsState().value
    BackHandler {
        viewModel.leavePage { navController.popBackStack() }
        return@BackHandler
    }
    UsersScreen(
        onNaviUpClick = {
            viewModel.leavePage { navController.popBackStack() }
        },
        state = uiState,
        tempState = uiState,
        onAddUserClick = {},
        onRemoveUserClick = {},
    )
}

@Composable
fun UsersScreen(
    onNaviUpClick: () -> Unit,
    onAddUserClick: (Boolean) -> Unit,
    onRemoveUserClick: (Boolean) -> Unit,
    state: UsersUiState,
    tempState: UsersUiState,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        color = colorResource(id = R.color.primary),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )
    Scaffold(topBar = {
        BackTopAppBar(
            onNaviUpClick = onNaviUpClick,
            title = stringResource(id = R.string.lock_tab_bar_user_code)
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

        }
    }
}
@Preview
@Composable
private fun PreviewAutoUnlockScreen() {
    FuhsingSmartLockV2AndroidTheme {
        UsersScreen(
            onAddUserClick = { },
            onRemoveUserClick = { },
            onNaviUpClick = { },
            state = UsersUiState(),
            tempState = UsersUiState(),
        )
    }
}
