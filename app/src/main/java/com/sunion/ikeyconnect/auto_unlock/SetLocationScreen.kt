package com.sunion.ikeyconnect.auto_unlock

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.IKeyTopAppBar
import com.sunion.ikeyconnect.ui.component.LoadingScreenDialog
import com.sunion.ikeyconnect.ui.component.PrimaryButton

@Composable
fun SetLocationScreen(
    viewModel: SetLocationViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val state = viewModel.uiState.collectAsState().value
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                SetLocationUiEvent.SaveLocationFailed ->
                    Toast.makeText(context, "Save location failure", Toast.LENGTH_SHORT).show()
                SetLocationUiEvent.SaveLocationSuccess ->
                    navController.popBackStack()
            }
        }
    }

    SetLocationScreen(
        initLocation = viewModel.location?:LatLng(0.0,0.0),
        onNaviUpClick = navController::popBackStack,
        onConfirmClick = viewModel::setLocationToLock,
        onLocationChange = viewModel::setLocation
    )
    if (state.isLoading)
        LoadingScreenDialog("")
}

@SuppressLint("MissingPermission")
@Composable
fun SetLocationScreen(
    initLocation: LatLng,
    onNaviUpClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onLocationChange: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val markerState = rememberMarkerState(position = initLocation)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(initLocation, 16f, 30f, 0f)
    }

    LaunchedEffect(key1 = cameraPositionState.position) {
        onLocationChange(cameraPositionState.position.target)
    }
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(compassEnabled = false, mapToolbarEnabled = false)
        ) {
            Marker(
                state = MarkerState(position = cameraPositionState.position.target),
                icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_lock_location_center_picker)
            )
            Circle(
                center = cameraPositionState.position.target,
                fillColor = colorResource(id = R.color.geofence_radius_fill_color),
                strokeColor = Color.Transparent,
                radius = 100.0,
            )
        }
        Column {
            IKeyTopAppBar(
                onNaviUpClick = onNaviUpClick,
                title = stringResource(id = R.string.toolbar_title_lock_location),
                backgroundColor = Color.Transparent
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_20)))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.space_18))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = colorResource(id = R.color.blue_info),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.space_14))
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_7)))
                Text(
                    text = stringResource(id = R.string.location_setup_description),
                    style = TextStyle(color = colorResource(id = R.color.blue_info))
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(id = R.drawable.ic_my_location),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.space_45))
                        .clickable {
                            cameraPositionState.position =
                                CameraPosition(initLocation, 16f, 30f, 0f)
                        }
                )
            }
        }

        PrimaryButton(
            text = stringResource(id = R.string.global_confirm),
            onClick = onConfirmClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = dimensionResource(id = R.dimen.space_24))
        )
    }
}
