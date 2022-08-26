package com.sunion.ikeyconnect.auto_unlock

interface AutoUnlockStatus {

    val macAddress: String
    data class AutoUnlockAlreadyUnlocked(override val macAddress: String) : AutoUnlockStatus
    data class AutoUnlockOperationSuccess(override val macAddress: String) : AutoUnlockStatus
    data class AutoUnlockConnectionFail(override val macAddress: String) : AutoUnlockStatus
    data class AutoUnlockOperationFail(override val macAddress: String, val message: String) : AutoUnlockStatus
    data class AutoUnlockBLENotEnabled(override val macAddress: String) : AutoUnlockStatus
    data class AutoUnlockLocationPermissionMissing(override val macAddress: String) : AutoUnlockStatus
    data class AutoUnlockLocationServiceNotEnabled(override val macAddress: String) : AutoUnlockStatus
    data class AutoUnlockScanError(override val macAddress: String, val message: String) : AutoUnlockStatus
    data class AutoUnlockTimeout(override val macAddress: String) : AutoUnlockStatus
}
