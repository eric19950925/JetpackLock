package com.sunion.ikeyconnect.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.usecase.home.UserSyncOrder
import com.sunion.ikeyconnect.home.component.Empty
import com.sunion.ikeyconnect.home.component.Locks
import com.sunion.ikeyconnect.ui.component.LoadingScreenDialog
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeViewModel, navController: NavController){
    val uiState = viewModel.uiState.collectAsState().value

    HomeScreen(
        state = uiState,
        onAddLockClick = {
            navController.navigate(HomeRoute.AddLock.route)
        },
        onPersonClick = {
            navController.navigate(HomeRoute.MemberManagement.route)
        },
        onLockClick = viewModel::onLockClick,
        onShowGuideClick = viewModel::setGuideHasBeenSeen,
        onSettingClick = { deviceIdentity ->
            navController.navigate(
                "${HomeRoute.Settings.route}/$deviceIdentity/${viewModel.isConnected(deviceIdentity)}/${viewModel.getBattery(deviceIdentity)}/${viewModel.getDeviceType(deviceIdentity)}"
            )
        },
        onAutoUnlockClick = { deviceIdentity ->
            navController.navigate(
                "${HomeRoute.AutoUnLock.route}/$deviceIdentity/${viewModel.getDeviceType(deviceIdentity)}"
            )
        },
        onUsersClick = { deviceIdentity ->
            navController.navigate(
                "${HomeRoute.Users.route}/$deviceIdentity/${viewModel.getDeviceType(deviceIdentity)}"
            )
        },
        onUserCodeClick = { deviceIdentity ->
            navController.navigate(
                "${HomeRoute.UserCode.route}/$deviceIdentity/${viewModel.getDeviceType(deviceIdentity)}"
            )
        },
        currentPage = uiState.currentPage,
        getUpdateTime = viewModel::getUpdateTime,
        onLockNameChange = viewModel::setLockName,
        onPageChangeByUser = viewModel::setCurrentPage,
        onSaveNameClick = viewModel::saveName,
        onDrawerStateChange = { isOpen ->
            viewModel.onDrawer(isOpen)
        },
        onListReOrder = { list ->
            viewModel.changeOrderList(list)
        }
    )
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAddLockClick: () -> Unit,
    onPersonClick: () -> Unit,
    onShowGuideClick: () -> Unit,
    onSettingClick: (String) -> Unit,
    onAutoUnlockClick: (String) -> Unit,
    onUsersClick: (String) -> Unit,
    onUserCodeClick: (String) -> Unit,
    onLockClick: () -> Unit,
    getUpdateTime: (String) -> Int?,
    onLockNameChange: (String) -> Unit,
    currentPage: Int,
    boltToastState: Boolean? = null,
    onPageChangeByUser: (Int) -> Unit = {},
    onDrawerStateChange: (Boolean) -> Unit = {},
    onListReOrder: (MutableList<UserSyncOrder>) -> Unit = {},
    onSaveNameClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val pagerState = rememberPagerState()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChangeByUser(page)
        }
    }

    LaunchedEffect(key1 = currentPage) {
        pagerState.animateScrollToPage(currentPage)
    }

    LaunchedEffect(scaffoldState){
        snapshotFlow { scaffoldState.drawerState.isOpen }.collect { isOpen ->
            onDrawerStateChange(isOpen)
        }
    }

    BackHandler {
        return@BackHandler
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_20)))
                    Image(
                        painter = painterResource(id = R.drawable.ic_list),
                        contentDescription = "List",
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_32))
                            .clickable { coroutineScope.launch { scaffoldState.drawerState.open() } }
                    )
                },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_circle),
                        contentDescription = "Add",
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_32))
                            .clickable(onClick = onAddLockClick)
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_8)))
                    Image(
                        painter = painterResource(id = R.drawable.ic_person_circle),
                        contentDescription = "Person",
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_32))
                            .clickable(onClick = onPersonClick)
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_20)))
                },
                title = {},
                backgroundColor = if(state.networkAvailable)MaterialTheme.colors.primary else colorResource(id = R.color.disconnected)
            )
        },
        drawerContent = {
            DrawerView(state.lockOrder, onListReOrder)
        },
        drawerBackgroundColor = Color.White,
        modifier = modifier
    ) { contentPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.White).padding(contentPadding), contentAlignment = Alignment.Center) {
            if (state.locks.isEmpty() && !state.isLoading)
                Empty(modifier = Modifier.align(Alignment.Center))
            else
                Locks(
                    locks = state.locks,
                    pagerState = pagerState,
                    onAutoUnlockClock = onAutoUnlockClick,
                    onUsersClick = onUsersClick,
                    onUserCodeClick = onUserCodeClick,
                    onSettingClick = onSettingClick,
                    onLockClick = onLockClick,
                    onLockNameChange = onLockNameChange,
                    getUpdateTime = getUpdateTime,
                    onSaveNameClick = onSaveNameClick,
                    networkAvailable = state.networkAvailable,
                    loadingLocks = state.loadingLocks,
                )
        }

    }

    if (state.showGuide)
        HomeGuideScreen(modifier = Modifier.clickable(onClick = onShowGuideClick))

    if (state.isLoading)
        LoadingScreenDialog(state.loadingMessage)
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        HomeScreen(
            state = HomeUiState(),
            onAddLockClick = {},
            onPersonClick = {},
            onShowGuideClick = {},
            onSettingClick = {},
            onAutoUnlockClick = {},
            onUsersClick = {},
            onUserCodeClick = {},
            onLockClick = {},
            currentPage = 0,
            getUpdateTime = { 2 },
            onLockNameChange = { _ -> },
            boltToastState = false,
            onSaveNameClick = {}
        )
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun Preview2() {
    FuhsingSmartLockV2AndroidTheme {
        HomeScreen(
            state = HomeUiState(),
            onAddLockClick = {},
            onPersonClick = {},
            onShowGuideClick = {},
            onSettingClick = {},
            onAutoUnlockClick = {},
            onUsersClick = {},
            onUserCodeClick = {},
            onLockClick = {},
            currentPage = 0,
            getUpdateTime = { 2 },
            onLockNameChange = { _ -> },
            boltToastState = false,
            onSaveNameClick = {}
        )
    }
}


@Preview(device = Devices.PIXEL_C)
@Composable
private fun Preview3() {
    FuhsingSmartLockV2AndroidTheme {
        HomeScreen(
            state = HomeUiState(),
            onAddLockClick = {},
            onPersonClick = {},
            onShowGuideClick = {},
            onSettingClick = {},
            onAutoUnlockClick = {},
            onUsersClick = {},
            onUserCodeClick = {},
            currentPage = 0,
            onLockClick = {},
            getUpdateTime = { 2 },
            onLockNameChange = { _ -> },
            boltToastState = false,
            onSaveNameClick = {}
        )
    }
}