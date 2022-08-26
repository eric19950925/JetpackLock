package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface Lock {
    val lockInfo: LockInfo
    val connectionState: SharedFlow<Event<Pair<Boolean, String>>>

    fun connect()

    fun disconnect()

    fun isConnected(): Boolean

    fun getName(shouldSave: Boolean = false): Flow<String>

    fun setTime(epochSecond: Long): Flow<Boolean>

    suspend fun getLockName(): String

    suspend fun hasAdminCodeBeenSet(): Boolean

    suspend fun changeLockNameByBle(name: String): Boolean

    suspend fun setTimeZoneByBle(timezone: String): Boolean

    suspend fun changeAdminCode(
        thingName: String,
        code: String,
        lockName: String,
        timezone: String,
        clientToken: String,
    ): Boolean

    suspend fun setAdminCodeByBle(
        macAddress: String,
        code: String,
        userName: String,
        clientToken: String,
    ): Boolean

    suspend fun editToken(index: Int, permission: String, name: String): Boolean

    suspend fun setLocation(thingName: String, latitude: Double, longitude: Double, clientToken: String): Boolean

    suspend fun setLocationByBle(latitude: Double, longitude: Double): LockConfig

    suspend fun getBoltOrientationByBle(): LockSetting

    suspend fun getLockSetting(): Flow<LockSetting>

    suspend fun getLockConfigByBle(): LockConfig

    suspend fun getLockConfig(thingName: String, clientToken: String): LockConfig

    suspend fun delete(clientToken: String? = null)

    suspend fun lockByBle(): LockSetting

    suspend fun unlockByBle(): LockSetting

    suspend fun changeAdminCodeByBle(oldCode: String, newCode: String): Boolean
}