package com.sunion.ikeyconnect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunion.ikeyconnect.account.AccountActivity
import com.sunion.ikeyconnect.account.AccountNavigation
import com.sunion.ikeyconnect.account.LoginViewModel
import com.sunion.ikeyconnect.auto_unlock.*
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.EventState
import com.sunion.ikeyconnect.home.HomeNavHost
import com.sunion.ikeyconnect.home.HomeViewModel
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
    OnCompleteListener<Void> {
    @Inject
    lateinit var statefulConnection: StatefulConnection

    @Inject
    lateinit var mqttManager: AWSIotMqttManager

    @Inject
    lateinit var mqttStatefulConnection: MqttStatefulConnection

    @Inject
    lateinit var geofenceTaskUseCase: PendingGeofenceTaskUseCase

    private val viewModel: AutoUnlockViewModel by viewModels()

    private var isGoSettingForLocationPermission = false
    /**
     * Used when requesting to add or remove geofences.
     */
    private var geofencePendingIntent: PendingIntent? = null

    private var pendingGeofenceTask: PendingGeofenceTask = PendingGeofenceTask.NONE

    @Inject lateinit var geofencingClient: GeofencingClient

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            FuhsingSmartLockV2AndroidTheme {
                NavigationComponent(
                    navController,
                    onLogoutClick = this::goLogin,
                    onLoginSuccess = this::goHome
                )
            }
        }
        collectLockGeoOperation()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }


    }
    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        }else{
            //deny
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Timber.d("${it.key} = ${it.value}")
            }
        }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
        try {
            mqttStatefulConnection.unsubscribeAllTopic()
            mqttManager.disconnect()
            Timber.d("mqttDisconnect success.")
        }catch (e: Exception){
            Timber.e( "mqttDisconnect error: $e")
        }
    }

    override fun onRestart() {
        super.onRestart()
        Timber.d("onRestart")
        try {
            mqttStatefulConnection.connectMqtt()
            Timber.d("mqttConnecting...")
        }catch (e: Exception){
            Timber.e( "mqttConnect error: $e")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttStatefulConnection.unsubscribeAllTopic()
            mqttManager.disconnect()
            statefulConnection.close()
            Timber.d("mqttDisconnect success.")
        }catch (e: Exception){
            Timber.e( "mqttDisconnect error: $e")
        }
    }

    companion object {
        const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"
        const val REQUEST_ENABLE_LOCATION_PERMISSION = 666
        const val REQUEST_ENABLE_BT = 777
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        var MY_PENDING_INTENT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
    private fun goLogin() {
        startActivity(Intent(this, AccountActivity::class.java))
        finish()
    }
    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun collectLockGeoOperation() {
        lifecycleScope.launch {
            viewModel.lockGeoInformation.collect{ event ->
                val triple = event.getContentIfNotHandled()
                when (event.status) {
                    EventState.SUCCESS -> {
                        triple?.let { config ->
                            if (!checkPermissions()) {
//                                    FirebaseAnalytics.getInstance(this).logEvent("geo_operation_permission_insufficient", null)
//                                    snackbar(toolbar_home, getString(R.string.global_insufficient_permissions))
                                Toast.makeText(this@MainActivity, getString(R.string.android_permission_not_insufficient), Toast.LENGTH_SHORT).show()
                            } else {
                                when (config.third) {
                                    is PendingGeofenceTask.ADD -> {
                                        Timber.d("add geo")
                                        addLockGeofences(config.first to config.second)
                                    }
                                    is PendingGeofenceTask.REMOVE -> {
                                        Timber.d("remove geo")
                                        removeLockGeofences(config.first to config.second)
                                    }
                                }
                            }
                        }
                    }
                    EventState.ERROR -> Toast.makeText(this@MainActivity, getString(R.string.android_geo_fence_operation_fail), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     *
     */
    fun performPendingGeofenceTask(isEnabled: Boolean, deviceIdentity: String?) {

        togglePendingGeofenceTask(isEnabled)

        if (!checkGeoTaskPermissions()) {
            Timber.e("location permission insufficient")
//            FirebaseAnalytics.getInstance(this).logEvent("geo_operation_permission_go_setting", null)
            isGoSettingForLocationPermission = true
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) accessBackgroundLocationPermissionApi30() else accessBackgroundLocationPermissionApi29()
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(
                    this,
                    REQUEST_ENABLE_LOCATION_PERMISSION,
                    *permissions
                )
                    .setTheme(R.style.Theme_iKey_Permission_MaterialAlertDialog_FilledButton)
                    .setRationale("This app collects location data to enable \"AutoUnlock\" even when the app is closed or not in use.")
                    .build()
            )
        } else {
            viewModel.getLockGeoInformation((if (isEnabled) PendingGeofenceTask.ADD else PendingGeofenceTask.REMOVE), deviceIdentity)
        }
    }

    private fun togglePendingGeofenceTask(isEnabled: Boolean) {
        if (isEnabled) {
//            FirebaseAnalytics.getInstance(this).logEvent("geo_operation_toggle_on", null)
            Timber.d("toggle auto unlock: $isEnabled, PendingGeofenceTask.ADD")
            pendingGeofenceTask = PendingGeofenceTask.ADD
        } else {
//            FirebaseAnalytics.getInstance(this).logEvent("geo_operation_toggle_off", null)
            Timber.d("toggle auto unlock: $isEnabled, PendingGeofenceTask.REMOVE")
            pendingGeofenceTask = PendingGeofenceTask.REMOVE
        }
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     *
     */
    @SuppressLint("MissingPermission")
    private fun addLockGeofences(config: Pair<LatLng, String>) {

        try {
//            FirebaseAnalytics.getInstance(this).logEvent(
//                "geo_operation_add",
//                bundleOf(
//                    "latlng" to "${config.first.latitude}, ${config.first.longitude}",
//                    "macAddress" to config.second
//                )
//            )
        } catch (error: Throwable) {
//            FirebaseCrashlytics.getInstance().recordException(error)
            Timber.e(error)
        }

        // Add the geofences to be monitored by geofencing service.
        val geofence = Geofence.Builder() // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(config.second.toUpperCase(Locale.ROOT)) // Set the circular region of this geofence.
            .setCircularRegion(config.first.latitude, config.first.longitude, GEOFENCE_RADIUS_IN_METERS) // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT) // Create the geofence.
            .build()
        val listOfFences = listOf(geofence)
        geofencingClient
            .addGeofences(buildGeofencingRequest(listOfFences), getGeofencePendingIntent()?: throw Exception("getGeofencePendingIntentNull"))
            .addOnCompleteListener(this)
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     *
     */
    fun removeLockGeofences(config: Pair<LatLng, String>) {
        Timber.d("task: $pendingGeofenceTask")

        try {
//            FirebaseAnalytics.getInstance(this).logEvent(
//                "geo_operation_remove",
//                bundleOf(
//                    "latlng" to "${config.first.latitude}, ${config.first.longitude}",
//                    "macAddress" to config.second
//                )
//            )
        } catch (error: Throwable) {
//            FirebaseCrashlytics.getInstance().recordException(error)
            Timber.e(error)
        }

        val ids = mutableListOf(config.second)
        Timber.d("removed fences of $ids, task: $pendingGeofenceTask")
        geofencingClient
            .removeGeofences(ids)
            .addOnCompleteListener(this)
    }

    /**
     * Runs when the result of calling [.addGeofences] and/or [.removeGeofences]
     * is available.
     * @param task the resulting Task, containing either a result or error.
     *
     */
    override fun onComplete(task: Task<Void>) {

        if (task.isSuccessful) {
            Timber.d("PendingGeofenceTask isSuccessful")
//            FirebaseAnalytics.getInstance(this).logEvent("geo_operation_success", null)
            flow { emit(geofenceTaskUseCase.emitGeofenceTask(task, pendingGeofenceTask, "")) }
                .onCompletion {
                    pendingGeofenceTask = PendingGeofenceTask.NONE
                }
                .flowOn(Dispatchers.IO)
                .launchIn(scope)
        } else {
            // Get the status code for the error and log it using a user-friendly message.
//            FirebaseAnalytics.getInstance(this).logEvent("geo_operation_error", null)
            val msg = GeofenceErrorMessages.getErrorString(this, task.exception)
            flow { emit(geofenceTaskUseCase.emitGeofenceTask(task, pendingGeofenceTask, msg)) }
                .flowOn(Dispatchers.IO)
                .launchIn(scope)
            Timber.e(GeofenceErrorMessages.getErrorString(this, task.exception))
            Toast.makeText(this, "Geofence operation error", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                Toast.makeText(this, "Please make sure Location permission is Allow all the time", Toast.LENGTH_SHORT).show()
//                FirebaseAnalytics.getInstance(this).logEvent("geo_operation_permission_error", null)
            }
            pendingGeofenceTask = PendingGeofenceTask.NONE
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the Service that handles geofence transitions.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getGeofencePendingIntent(): PendingIntent? {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent
        }
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)

        geofencePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return geofencePendingIntent
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     *
     */
    private fun buildGeofencingRequest(listOfFences: List<Geofence> = emptyList()): GeofencingRequest {
        val builder = GeofencingRequest.Builder()

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(0)

        Timber.d("add fences of: $listOfFences")
        builder.addGeofences(listOfFences)

        // Return a GeofencingRequest.
        return builder.build()
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun accessBackgroundLocationPermissionApi29(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun accessBackgroundLocationPermissionApi30(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    /**
     * Returns the required permissions of the Geofencing operation.
     */
    fun checkGeoTaskPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(this, *accessBackgroundLocationPermissionApi30())
        } else {
            EasyPermissions.hasPermissions(this, *accessBackgroundLocationPermissionApi29())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_ENABLE_LOCATION_PERMISSION) {
            Timber.d("perms has been granted: $perms")
            flow { emit(viewModel.autoUnlockPermissionResult.emit(Event.success(true))) }
                .flowOn(Dispatchers.IO)
                .launchIn(scope)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_ENABLE_LOCATION_PERMISSION) {
            flow { emit(viewModel.autoUnlockPermissionResult.emit(Event.success(false))) }
                .flowOn(Dispatchers.IO)
                .launchIn(scope)
        }
    }
}

@Composable
fun NavigationComponent(navController: NavHostController, onLogoutClick: () -> Unit, onLoginSuccess: () -> Unit) {
    val loginViewModel = viewModel<LoginViewModel>()
    val homeViewModel = viewModel<HomeViewModel>()
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                loginViewModel,
                toHome = {
                    navController.navigate("home")
                    loginViewModel.setCredentialsProvider()
                },
                toLogin = {
                    navController.navigate("login")
                },
                logOut = {
                    loginViewModel.logOut()
                }
            )
        }
        composable("login") {
            AccountNavigation(
                onLoginSuccess= {
                    loginViewModel.setAttachPolicy()
                    onLoginSuccess.invoke()
                                }
            )
        }
        composable("home") {
            HomeNavHost(
                homeViewModel,
                onLogoutClick = onLogoutClick
            )
        }
    }
}
