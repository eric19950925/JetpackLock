package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.sunion.ikeyconnect.R

@Composable
fun GoogleMapViewOnly(latLng: LatLng, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(latLng, 16f, 30f, 0f)
    }
    GoogleMap(
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            rotationGesturesEnabled = false,
            mapToolbarEnabled = false,
            scrollGesturesEnabled = false,
            scrollGesturesEnabledDuringRotateOrZoom = false,
            tiltGesturesEnabled = false,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = false
        ),
        modifier = modifier
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
}

@Preview
@Composable
private fun Preview() {
    GoogleMapViewOnly(
        latLng = LatLng(25.0330, 121.5654),
        modifier = Modifier.fillMaxSize()
    )
}