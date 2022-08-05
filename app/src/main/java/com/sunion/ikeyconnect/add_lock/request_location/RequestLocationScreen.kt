package com.sunion.ikeyconnect.add_lock.request_location

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import timber.log.Timber
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.add_lock.component.AddLockScreenScaffold
import com.sunion.ikeyconnect.add_lock.component.ExitPromptDialog
import com.sunion.ikeyconnect.home.HomeRoute
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun RequestLocationScreen(viewModel: RequestLocationViewModel, navController: NavController) {
    var showExitPromptDialog by remember { mutableStateOf(false) }
    val locationPermissionRequest =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                }
                else -> {
                    // No location access granted.
                }
            }
        }
    BackHandler { showExitPromptDialog = false }

    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                RequestLocationUiEvent.DeleteLockFail -> Timber.e("Cannot delete lock")
                RequestLocationUiEvent.DeleteLockSuccess ->
                    navController.popBackStack(HomeRoute.Home.route, false)
            }
        }
    }

    RequestLocationScreen(
        onNaviUpClick = { showExitPromptDialog = true },
        onSetUpLocationClick = {
            if (!viewModel.hasLocationPermission()) {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else
                navController.navigate("${AddLockRoute.SetLocation.route}/${viewModel.macAddress}")
        },
        onSkipClick = { navController.navigate("${AddLockRoute.LockOverview.route}/${viewModel.macAddress}") })

    if (showExitPromptDialog)
        ExitPromptDialog(
            onDismissRequest = { showExitPromptDialog = false },
            onConfirmButtonClick = {
                showExitPromptDialog = false
                viewModel.deleteLock()
            },
            onDismissButtonClick = { showExitPromptDialog = false }
        )
}

@Composable
fun RequestLocationScreen(
    onNaviUpClick: () -> Unit,
    onSetUpLocationClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AddLockScreenScaffold(
        onNaviUpClick = onNaviUpClick,
        title = stringResource(id = R.string.toolbar_title_add_lock),
        step = 5,
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = dimensionResource(id = R.dimen.space_20)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_64)))
        Image(
            painter = painterResource(id = R.drawable.ic_location_permission),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            modifier = Modifier.size(dimensionResource(id = R.dimen.space_200))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_32)))
        Text(
            text = stringResource(id = R.string.location_permission_title),
            style = TextStyle(color = MaterialTheme.colors.primary)
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_16)))
        Text(
            text = stringResource(id = R.string.location_permission_content),
            style = TextStyle(color = MaterialTheme.colors.primaryVariant, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.global_skip),
            style = TextStyle(
                color = MaterialTheme.colors.primaryVariant,
                fontSize = 14.sp,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.space_48))
                .clickable(onClick = onSkipClick)
                .wrapContentSize()
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_20)))
        PrimaryButton(
            text = stringResource(id = R.string.gloabl_setup_location),
            onClick = onSetUpLocationClick
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
    }
}

@Preview
@Composable
private fun PreviewDefault() {
    FuhsingSmartLockV2AndroidTheme {
        RequestLocationScreen({}, onSetUpLocationClick = {}, {})
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun PreviewSmall() {
    FuhsingSmartLockV2AndroidTheme {
        RequestLocationScreen({}, onSetUpLocationClick = {}, {})
    }
}