package com.sunion.ikeyconnect.auto_unlock

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sunion.ikeyconnect.MainActivity
import com.sunion.ikeyconnect.data.DeviceDatabase
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.Locale
import timber.log.Timber

@AndroidEntryPoint
class BootBroadcastReceiver : DaggerBroadcastReceiver() {

    lateinit var geofencingClient: GeofencingClient

    private var geofencePendingIntent: PendingIntent? = null

    @SuppressLint("MissingPermission", "CheckResult")
    override fun onReceive(context: Context, intent: Intent) {

        geofencingClient = LocationServices.getGeofencingClient(context.applicationContext)

        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
//            FirebaseAnalytics.getInstance(context).logEvent("reboot_geo_permission_insufficient", null)
            Timber.d("ACTION_BOOT_COMPLETED action received, re-register geofence event")

            DeviceDatabase
                .get(context.applicationContext)
                .lockConnectionInformationDao()
                .getAllLockConnectionInformation()
                .map {
                    it
                        .takeIf { it.isNotEmpty() }
                        ?.filter { lock -> lock.isAutoUnlockOn }
                        ?: emptyList()
                }
                .subscribeOn(Schedulers.io())
                .doOnSuccess { autoUnlockEnableLocks ->
                    if (!checkGeoTaskPermissions(context)) {
                        Timber.d("reboot_geo_permission_insufficient")
//                        FirebaseAnalytics.getInstance(context).logEvent("reboot_geo_permission_insufficient", null)
                    } else {
                        autoUnlockEnableLocks
                            .takeIf { it.isNotEmpty() }
                            ?.filter { it.latitude != null || it.latitude != 0.0 || it.longitude != null || it.longitude != 0.0 }
                            ?.map { lockConnectionInformation ->
                                val geofence =
                                    Geofence.Builder() // Set the request ID of the geofence. This is a string to identify this
                                        // geofence.
                                        .setRequestId(lockConnectionInformation.macAddress.toUpperCase(Locale.ROOT))
                                        // Set the circular region of this geofence.
                                        .setCircularRegion(
                                            lockConnectionInformation.latitude!!,
                                            lockConnectionInformation.longitude!!,
                                            MainActivity.GEOFENCE_RADIUS_IN_METERS
                                        ) // Set the expiration duration of the geofence. This geofence gets automatically
                                        // removed after this period of time.
                                        .setExpirationDuration(Geofence.NEVER_EXPIRE) // Set the transition types of interest. Alerts are only generated for these
                                        // transition. We track entry and exit transitions in this sample.
                                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT) // Create the geofence.
                                        .build()
                                val listOfFences = listOf(geofence)

                                geofencingClient
                                    ?.addGeofences(
                                        GeofencingRequest
                                            .Builder()
                                            .setInitialTrigger(0)
                                            .addGeofences(listOfFences)
                                            .build(),
                                        getGeofencePendingIntent(context)?:throw Exception("GeofencePendingNull")
                                    )
                                    ?.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
//                                            FirebaseAnalytics.getInstance(context).logEvent("reboot_geo_operation_done", null)
                                            Timber.d("reboot & re-register geofence task is successful")
                                        } else {
//                                            FirebaseAnalytics.getInstance(context).logEvent("reboot_geo_operation_error", null)
                                            Timber.e("reboot & re-register geofence task is failed")
                                        }
                                    }
                            }
                    }
                }
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Timber.d("get all locks that enable auto unlock from local database, $it")
                    },
                    {
                        Timber.e(it)
                    }
                )
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the Service that handles geofence transitions.
     */
    private fun getGeofencePendingIntent(context: Context): PendingIntent? {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent
        }
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)

        geofencePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return geofencePendingIntent
    }

    private fun checkGeoTaskPermissions(context: Context): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
