package com.sunion.ikeyconnect.auto_unlock

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.sunion.ikeyconnect.R

@Composable
fun SetLocationScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(LatLng(0.0, 0.0), 16f, 30f, 0f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
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
}
