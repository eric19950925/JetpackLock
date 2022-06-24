package com.sunion.ikeyconnect.add_lock.lock_overview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.GoogleMapViewOnly
import com.sunion.ikeyconnect.ui.component.PrimaryButton
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryBoldSize18

@Composable
fun LockOverviewScreen(viewModel: LockOverviewViewModel, onCompleteClick: () -> Unit) {
    BackHandler {/* do nothing */ }
    val uiState = viewModel.uiState.collectAsState().value
    LockOverviewScreen(
        state = uiState,
        onCompleteClick = onCompleteClick
    )
}

@Composable
fun LockOverviewScreen(
    state: LockOverviewUiState,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.toolbart_title_lock_overview),
            style = MaterialTheme.typography.colorPrimaryBoldSize18,
            modifier = Modifier
                .height(44.dp)
                .fillMaxWidth()
                .wrapContentSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.space_285))
                .background(color = colorResource(id = R.color.light_primary)),
            contentAlignment = Alignment.Center
        ) {
            if (state.location == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_skip_content_image),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_16)))
                    Text(
                        text = stringResource(id = R.string.lock_without_location),
                        style = TextStyle(color = MaterialTheme.colors.primaryVariant)
                    )
                }
            } else
                GoogleMapViewOnly(
                    latLng = state.location,
                    modifier = Modifier.fillMaxSize()
                )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.space_28),
                    vertical = dimensionResource(id = R.dimen.space_25)
                )
        ) {
            Text(
                text = stringResource(id = R.string.lock_overview_lock_name, state.lockName),
                style = TextStyle(color = MaterialTheme.colors.primary, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_23)))
            Text(
                text = stringResource(id = R.string.lock_overview_user_name, state.userName),
                style = TextStyle(color = MaterialTheme.colors.primary, fontSize = 14.sp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(id = R.string.global_complete),
            onClick = onCompleteClick
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_24)))
    }
}

@Preview
@Composable
private fun PreviewDefault() {
    FuhsingSmartLockV2AndroidTheme {
        LockOverviewScreen(
            state = LockOverviewUiState(
                lockName = "Front door lock",
                userName = "Eric",
                location = LatLng(25.0330, 121.5654)
            ),
            onCompleteClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewWithSkip() {
    FuhsingSmartLockV2AndroidTheme {
        LockOverviewScreen(
            state = LockOverviewUiState(
                lockName = "Front door lock",
                userName = "Eric"
            ),
            onCompleteClick = {}
        )
    }
}