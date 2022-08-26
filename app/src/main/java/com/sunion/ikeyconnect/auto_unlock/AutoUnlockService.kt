package com.sunion.ikeyconnect.auto_unlock

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.sunion.ikeyconnect.MainActivity
import com.sunion.ikeyconnect.MainActivity.Companion.MY_PENDING_INTENT_FLAG
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.Scheduler
import com.sunion.ikeyconnect.data.PreferenceStore
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import timber.log.Timber

/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 *
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 *
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification associated with that service is removed.
 *
 */
@AndroidEntryPoint
class AutoUnlockService : Service() {

    private val mBinder: IBinder = LocalBinder()

    @Inject
    lateinit var autoUnlockUseCase: AutoUnlockUseCase
    @Inject
    lateinit var preferenceStore: PreferenceStore
    @Inject
    lateinit var statefulConnection: StatefulConnection
    @Inject
    lateinit var scheduler: Scheduler
    @Inject
    lateinit var lockInformationRepository: LockInformationRepository

    private val connectionDisposable = CompositeDisposable()

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     *
     */
    private var mChangingConfiguration = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")

        autoUnlockUseCase
            .status
            .toFlowable(BackpressureStrategy.LATEST)
            .flatMap { status ->
                lockInformationRepository
                    .get(macAddress = status.macAddress)
                    .toFlowable()
                    .map { lock ->
                        if (status !is AutoUnlockStatus.AutoUnlockAlreadyUnlocked) {
                            when (status) {
                                is AutoUnlockStatus.AutoUnlockOperationSuccess -> getString(R.string.geofence_success_notification, lock.displayName)
                                is AutoUnlockStatus.AutoUnlockConnectionFail -> getString(R.string.geofence_invalid_token_notification, lock.displayName)
                                is AutoUnlockStatus.AutoUnlockTimeout -> getString(R.string.geofence_timeout_notification, lock.displayName)
                                is AutoUnlockStatus.AutoUnlockLocationServiceNotEnabled -> getString(R.string.geofence_location_not_enabled, lock.displayName)
                                is AutoUnlockStatus.AutoUnlockLocationPermissionMissing -> getString(R.string.geofence_location_permission_not_enabled, lock.displayName)
                                is AutoUnlockStatus.AutoUnlockBLENotEnabled -> getString(R.string.geofence_bluetooth_disable_notification, lock.displayName)
                                is AutoUnlockStatus.AutoUnlockScanError -> status.message
                                is AutoUnlockStatus.AutoUnlockOperationFail -> status.message
                                // including AutoUnlockStatus.AutoUnlockOperationFail, AutoUnlockScanError
                                else -> getString(R.string.geofence_timeout_notification, lock.displayName)
                            }
                        } else {
                            ""
                        }
                    }
                    .subscribeOn(scheduler.io())
            }
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.main())
            .subscribe(
                { notificationString ->
                    notificationString.takeIf { it.isNotEmpty() }?.let { sendNotification(it) }
//                    autoUnlockUseCase.reconnectDisposable?.dispose()
                    stopForeground(true)
                    stopSelf()

                    notificationString.takeIf { it.isNotEmpty() }?.let {
                        sendNotification(it)

                        try {
//                            FirebaseAnalytics.getInstance(this).logEvent(
//                                "auto_unlock_status",
//                                bundleOf("result" to notificationString)
//                            )
                        } catch (error: Throwable) {
//                            FirebaseCrashlytics.getInstance().recordException(error)
                            Timber.e(error)
                        }
                    }
                },
                { error ->
                    Timber.e(error)
//                    FirebaseCrashlytics.getInstance().recordException(error)
                }
            )
            .apply { connectionDisposable.add(this) }
    }

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int
    ): Int {
        Timber.i("Service onStartCommand")

        startForeground(APP_FOREGROUND_NOTIFICATION_ID, getNotification())

        val startedFromNotification = intent.getBooleanExtra(
            EXTRA_STARTED_FROM_NOTIFICATION,
            false
        )

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            stopForeground(true)
            stopSelf()
        } else {
            autoUnlockUseCase.invoke {
                stopForeground(true)
                stopSelf()
            }
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onBind(intent: Intent): IBinder? {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Timber.i("in onBind()")
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Timber.i("in onRebind()")
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.i("Last client unbound from service")

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration) {
            Timber.i("Starting foreground service")
            startForeground(APP_FOREGROUND_NOTIFICATION_ID, getNotification())
        }
        return true // Ensures onRebind() is called when a client re-binds.
    }

    override fun onDestroy() {
        connectionDisposable.clear()
    }

    /**
     * Returns the [NotificationCompat] used as part of the foreground service.
     */
    fun getNotification(): Notification {
        val intent = Intent(this, AutoUnlockService::class.java)
        val text = getString(R.string.app_name)

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        val servicePendingIntent =
            PendingIntent.getService(
                this, 0, intent,
                MY_PENDING_INTENT_FLAG
            )

        // The PendingIntent to launch activity.
        val activityPendingIntent =
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java), MY_PENDING_INTENT_FLAG
            )
        val builder = NotificationCompat.Builder(
            this,
            APP_FOREGROUND_CHANNEL_ID
        )
            .addAction(
                R.drawable.ic_lock_main, "Launch iKey",
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_error, "Stop Auto Unlock",
                servicePendingIntent
            )
            .setContentText(text)
            .setContentTitle("iKey Auto Unlock is running")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(text)
            .setWhen(System.currentTimeMillis())

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(APP_FOREGROUND_CHANNEL_ID) // Channel ID
        }

        return builder.build()
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the HomeActivity.
     *
     */
    private fun sendNotification(
        notificationDetails: String
    ) {
        // Get an instance of the Notification manager
        val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create an explicit content Intent that starts the main Activity.
        val notificationIntent = Intent(
            this,
            MainActivity::class.java
        )

        // Construct a task stack.
        val stackBuilder = TaskStackBuilder.create(this)

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity::class.java)

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent)

        // Get a PendingIntent containing the entire back stack.
        val notificationPendingIntent =
            stackBuilder.getPendingIntent(0, MY_PENDING_INTENT_FLAG)

        // Get a notification builder that's compatible with platform versions >= 4
        val builder = NotificationCompat.Builder(
            this,
            GeofenceBroadcastReceiver.AUTO_UNLOCK_CHANNEL_ID
        )

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_lock_main)
            // to decode the Bitmap.
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_lock_main))
            .setContentTitle("Auto-unlock")
            .setContentText(notificationDetails)
            .setContentIntent(notificationPendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationDetails)
            )

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(GeofenceBroadcastReceiver.AUTO_UNLOCK_CHANNEL_ID) // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true)

        // Issue the notification
        // Update notification content if running as a foreground service.
        mNotificationManager.notify(GeofenceBroadcastReceiver.AUTO_UNLOCK_NOTIFICATION_ID, builder.build())
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: AutoUnlockService
            get() = this@AutoUnlockService
    }

    companion object {
        private const val PACKAGE_NAME = "com.sunion.ikeyconnect"

        /**
         * The name of the channel for foreground service notifications.
         */
        const val APP_FOREGROUND_CHANNEL_ID = "$PACKAGE_NAME.foreground_service"

        private const val EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification"

        /**
         * The identifier for the notification displayed for the foreground service.
         */
        const val APP_FOREGROUND_NOTIFICATION_ID = 20210621
    }
}
