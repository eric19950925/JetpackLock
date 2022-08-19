package com.sunion.ikeyconnect.domain.model

data class LockConfig(
    val orientation: LockOrientation,
    val isSoundOn: Boolean,
    val isVacationModeOn: Boolean,
    val isAutoLock: Boolean,
    val autoLockTime: Int,
    val isPreamble: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null
)
