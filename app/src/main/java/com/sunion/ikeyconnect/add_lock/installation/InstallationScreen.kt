package com.sunion.ikeyconnect.add_lock.installation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.add_lock.component.AddLockScreenScaffold
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun InstallationScreen(
    navController: NavController,
    macAddress: String,
    modifier: Modifier = Modifier
) {
    InstallationScreen(
        onNaviUpClick = navController::popBackStack,
        onWatchInstructionsClick = { navController.navigate("${AddLockRoute.InstallationInstructions.route}/$macAddress") },
        modifier = modifier,
        onSkipClick = { navController.navigate("${AddLockRoute.Pairing.route}/$macAddress") }
    )
}

@Composable
fun InstallationScreen(
    onNaviUpClick: () -> Unit,
    onWatchInstructionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipClick: () -> Unit
) {
    AddLockScreenScaffold(
        onNaviUpClick = onNaviUpClick,
        title = stringResource(id = R.string.toolbar_title_installation),
        step = 2,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.ic_installation),
            contentDescription = null,
            modifier = Modifier.width(dimensionResource(id = R.dimen.space_254))
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.add_lock_installation_content_1),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary,
            )
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_16)))
        Text(
            text = stringResource(id = R.string.add_lock_installation_content_2),
            style = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colors.primary,
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(id = R.string.support_lock_install),
            onClick = onWatchInstructionsClick,
            modifier = Modifier.width(dimensionResource(id = R.dimen.space_243))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_16)))
        Text(
            text = stringResource(id = R.string.global_skip),
            modifier = Modifier.clickable(onClick = onSkipClick)
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
    }
}

@Preview
@Composable
private fun PreviewInstallationInstructionsScreen() {
    FuhsingSmartLockV2AndroidTheme {
        InstallationScreen(
            onNaviUpClick = {},
            onWatchInstructionsClick = {},
            onSkipClick = {}
        )
    }
}

@Preview(device = Devices.NEXUS_5X)
@Composable
private fun PreviewInstallationInstructionsScreenSmall() {
    FuhsingSmartLockV2AndroidTheme {
        InstallationScreen(
            onNaviUpClick = {},
            onWatchInstructionsClick = {}
        ) {}
    }
}