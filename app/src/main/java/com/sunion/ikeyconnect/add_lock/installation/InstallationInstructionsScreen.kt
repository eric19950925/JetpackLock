package com.sunion.ikeyconnect.add_lock.installation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.add_lock.component.AddLockTopAppBar
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R

@Composable
fun InstallationInstructionsScreen(
    viewModel: InstallationInstructionsViewModel,
    navController: NavController,
    macAddress: String,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value
    InstallationInstructionsScreen(
        onNaviUpClick = navController::popBackStack,
        step = uiState.step,
        instructions = uiState.instructions,
        onPreviousClick = viewModel::previous,
        onNextClick = viewModel::next,
        onCompleteClick = { navController.navigate("${AddLockRoute.Pairing.route}/$macAddress") },
        isLastStep = uiState.isLastStep,
        modifier = modifier
    )
}

@Composable
fun InstallationInstructionsScreen(
    onNaviUpClick: () -> Unit,
    step: String,
    instructions: Instructions,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCompleteClick: () -> Unit,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AddLockTopAppBar(
            title = stringResource(id = instructions.titleResId),
            onNaviUpClick = onNaviUpClick,
            step = step,
            totalStep = "9"
        )
        Video(
            videoPath = instructions.videoPath,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.space_280))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.space_32)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = instructions.descResId),
                style = TextStyle(
                    color = MaterialTheme.colors.primaryVariant,
                    fontSize = dimensionResource(id = R.dimen.txt_medium).value.sp
                )
            )
            Spacer(modifier = Modifier.weight(1f))

            if (isLastStep)
                PrimaryButton(
                    text = stringResource(id = R.string.global_complete),
                    onClick = onCompleteClick
                )
            else
                Row {
                    Image(
                        painter = painterResource(id = R.drawable.ic_video_previous),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_30))
                            .clickable(onClick = onPreviousClick)
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_24)))
                    Image(
                        painter = painterResource(id = R.drawable.ic_video_next),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_30))
                            .clickable(onClick = onNextClick)
                    )
                }
        }
    }
}

@Preview
@Composable
private fun PreviewInstallationInstructionsScreen() {
    FuhsingSmartLockV2AndroidTheme {
        InstallationInstructionsScreen(
            onNaviUpClick = {},
            step = "1",
            instructions = Instructions(
                titleResId = R.string.toolbar_title_lock_install_demo_1,
                descResId = R.string.install_demo_content_1,
                videoPath = "android.resource://${LocalContext.current.packageName}/${R.raw.installation_instructions_01}"
            ),
            onPreviousClick = {},
            onNextClick = {},
            onCompleteClick = {},
            isLastStep = false
        )
    }
}

@Preview(device = Devices.NEXUS_5X)
@Composable
private fun PreviewInstallationInstructionsScreenSmall() {
    FuhsingSmartLockV2AndroidTheme {
        InstallationInstructionsScreen(
            onNaviUpClick = {},
            step = "1",
            instructions = Instructions(
                titleResId = R.string.toolbar_title_lock_install_demo_1,
                descResId = R.string.install_demo_content_1,
                videoPath = "android.resource://${LocalContext.current.packageName}/${R.raw.installation_instructions_01}"
            ),
            onPreviousClick = {},
            onNextClick = {},
            onCompleteClick = {},
            isLastStep = true
        )
    }
}