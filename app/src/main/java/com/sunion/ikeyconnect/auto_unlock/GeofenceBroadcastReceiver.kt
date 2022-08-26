package com.sunion.ikeyconnect.auto_unlock

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.sunion.ikeyconnect.MainActivity
import com.sunion.ikeyconnect.MainActivity.Companion.MY_PENDING_INTENT_FLAG
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.Scheduler
import com.sunion.ikeyconnect.data.PreferenceStore
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import javax.inject.Inject
import timber.log.Timber

/**
 * Receiver for geofence transition changes.
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a JobIntentService
 * that will handle the intent in the background.
 *
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : DaggerBroadcastReceiver() {

    @Inject lateinit var autoUnlockUseCase: AutoUnlockUseCase
    @Inject lateinit var preferenceStore: PreferenceStore
    @Inject lateinit var lockInformationRepository: LockInformationRepository
    @Inject lateinit var scheduler: Scheduler
    @Inject lateinit var statefulConnection: StatefulConnection

    companion object {
        const val AUTO_UNLOCK_CHANNEL_ID = "auto_unlock_channel_id"

        /**
         * The identifier for the notification displayed for geofence service.
         */
        const val AUTO_UNLOCK_NOTIFICATION_ID = 6212021
    }

    /**
     * Receives incoming intents.
     *
     * @param context the application context.
     * @param intent sent by Location Services. This Intent is provided to Location
     * Services (inside a PendingIntent) when addGeofences() is called.
     */
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        super.onReceive(context, intent)

        val geofenceEvent = GeofencingEvent.fromIntent(intent)
        Timber.d("geofencingEvent, fences:${geofenceEvent?.triggeringGeofences?.first()?.requestId}, transition: ${geofenceEvent?.geofenceTransition}")
        if (geofenceEvent?.hasError() == true) {
            val errorMessage = GeofenceErrorMessages.getErrorString(
                context,
                geofenceEvent.errorCode
            )
            Timber.d(errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofenceEvent?.geofenceTransition

        Timber.d("onReceive geo broadcast a geo broadcast")

        try {
//            FirebaseAnalytics.getInstance(context).logEvent(
//                "auto_unlock_triggered",
//                bundleOf(
//                    "fences" to geofenceEvent.triggeringGeofences.joinToString { ", " },
//                    "transition" to geofenceEvent.geofenceTransition
//                )
//            )
        } catch (error: Throwable) {
            Timber.e(error)
//            FirebaseCrashlytics.getInstance().recordException(error)
        }

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            val triggeringGeofences = geofenceEvent.triggeringGeofences

            triggeringGeofences?.forEach {
                Timber.d("Geofence: ${it.requestId}")
            }

            // Get the transition details as a String.
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                context,
                geofenceTransition,
                triggeringGeofences?:throw Exception("triggeringGeofencesEmpty")
            )
            Timber.d("geofenceTransition: $geofenceTransition, isEnterNotify: ${preferenceStore.isEnterNotify}")

            geofenceEvent.triggeringGeofences.takeIf { it.isNullOrEmpty() }?.let {
                Observable
                    .fromIterable(geofenceEvent.triggeringGeofences)
                    .flatMap { geofence ->
                        lockInformationRepository.get(geofence.requestId).toObservable()
                    }
                    .subscribeOn(scheduler.io())
                    .doOnNext { lock ->
                        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER && lock.isEnterNotify) {
                            // Send notification and log the transition details.
                            sendNotification(context, geofenceTransitionDetails)
                        }

                        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT && lock.isExitNotify) {
                            // Send notification and log the transition details.
                            sendNotification(context, geofenceTransitionDetails)
                        }
                    }
                    .subscribeOn(scheduler.main())
                    .subscribe(
                        { lock ->
                            Timber.d("Lock: $lock checking if sending notification done.")
                        },
                        {
                            Timber.e(it)
                        }
                    )
            }

            Timber.d(geofenceTransitionDetails)

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                try {
                    autoUnlockUseCase.mac = geofenceEvent.triggeringGeofences!!.first().requestId
                    val serviceIntent = Intent(context, AutoUnlockService::class.java)
                    context.startService(serviceIntent)
                    Timber.d("GEOFENCE_TRANSITION_ENTER, start service")
                } catch (error: Throwable) {
                    Timber.d("GEOFENCE_TRANSITION_ENTER error")
//                    FirebaseCrashlytics.getInstance().recordException(error)
                    Timber.d(error)
                }
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Timber.d("GEOFENCE_TRANSITION_EXIT remove connection observable")
//                autoUnlockUseCase.removeConnectionObservable()
            }
        } else {
            // Log the error.
            Timber.d(
                context.getString(
                    R.string.geofence_transition_invalid_type,
                    geofenceTransition
                )
            )
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition The ID of the geofence transition.
     * @param triggeringGeofences The geofence(s) triggered.
     * @return The transition details formatted as String.
     */
    private fun getGeofenceTransitionDetails(
        context: Context,
        geofenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String {
        val geofenceTransitionString =
            getTransitionString(context, geofenceTransition)

        // Get the Ids of each geofence that was triggered.
        val triggeringGeofencesIdsList = ArrayList<String?>()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }
//        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)
        return geofenceTransitionString
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the HomeActivity.
     *
     */
    private fun sendNotification(
        context: Context,
        notificationDetails: String
    ) {
        // Get an instance of the Notification manager
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create an explicit content Intent that starts the main Activity.
        val notificationIntent = Intent(
            context,
            MainActivity::class.java
        )

        // Construct a task stack.
        val stackBuilder = TaskStackBuilder.create(context)

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity::class.java)

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent)

        // Get a PendingIntent containing the entire back stack.
        val notificationPendingIntent =
            stackBuilder.getPendingIntent(0, MY_PENDING_INTENT_FLAG)

        // Get a notification builder that's compatible with platform versions >= 4
        val builder = NotificationCompat.Builder(
            context,
            AUTO_UNLOCK_CHANNEL_ID
        )

        // The PendingIntent to launch activity.
        val activityIntent = PendingIntent.getActivity(
            context, 0,
            Intent(
                context,
                MainActivity::class.java
            ),
            MY_PENDING_INTENT_FLAG
        )
        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_lock_main)
            // to decode the Bitmap.
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_lock_main
                )
            )
            .setContentTitle(notificationDetails)
            .setContentText(context.getString(R.string.geofence_transition_notification_text))
            .setContentIntent(notificationPendingIntent)
            .addAction(R.drawable.ic_lock_main, "Launch iKey", activityIntent)

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(AUTO_UNLOCK_CHANNEL_ID) // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true)

        // Issue the notification
        // Update notification content if running as a foreground service.
        mNotificationManager.notify(AUTO_UNLOCK_NOTIFICATION_ID, builder.build())
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private fun getTransitionString(
        context: Context,
        transitionType: Int
    ): String {
        return when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> context.getString(
                R.string.geofence_transition_entered
            )
            Geofence.GEOFENCE_TRANSITION_EXIT -> context.getString(
                R.string.geofence_transition_exited
            )
            else -> context.getString(R.string.unknown_geofence_transition)
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The [Context].
     */
    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (
            service in manager.getRunningServices(
                Int.MAX_VALUE
            )
        ) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }
}
