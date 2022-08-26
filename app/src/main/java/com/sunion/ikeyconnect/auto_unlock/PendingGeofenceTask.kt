package com.sunion.ikeyconnect.auto_unlock

/**
 * Tracks whether the user requested to add or remove geofences, or to do neither.
 */
sealed class PendingGeofenceTask {
    object ADD : PendingGeofenceTask()
    object REMOVE : PendingGeofenceTask()
    object NONE : PendingGeofenceTask()
}
