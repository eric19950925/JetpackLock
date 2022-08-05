package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.data.SunionTraits
import com.sunion.ikeyconnect.data.getFirmwareModelTraits
import com.sunion.ikeyconnect.domain.Interface.ILockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.LockQRCodeParser
import com.sunion.ikeyconnect.domain.model.LockInfo
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.lock.WifiLock
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.rx2.asFlow
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockProvider @Inject constructor(
    private val lockInformationRepository : LockInformationRepository,
    private val wifiLock: WifiLock,
    private val provisionDomain: ProvisionDomain,
    private val getUuid: GetUuidUseCase,
) : ILockProvider {
//    private val locks = mutableMapOf<String, Lock>()


    override suspend fun getLockByMacAddress(macAddress: String): Lock? {
        //若已經拿過就可以拿暫存的，加速
//        if (locks.containsKey(macAddress))
//            return locks[macAddress]

        val lockConnectionInfo = runCatching {
            lockInformationRepository.get(macAddress).toObservable().asFlow().single()
        }.getOrNull() ?: return null

        val lockInfo = LockInfo.from(lockConnectionInfo)

        val lock: Lock = if (getFirmwareModelTraits(lockConnectionInfo.model).contains(SunionTraits.WiFi))
            wifiLock.init(lockInfo)
        else
            wifiLock.init(lockInfo)
//            BleLock(lockInfo)

//        locks[macAddress] = lock

        return lock
    }

    override suspend fun getLockByQRCode(content: String): Lock? {
        val qrCodeContent = runCatching { LockQRCodeParser.parseQRCodeContent(content) }.getOrNull()
            ?: runCatching { LockQRCodeParser.parseWifiQRCodeContent(content) }.getOrNull()
            ?: return null

        val lockInfo = LockInfo.from(qrCodeContent)

        //todo K1 will be FFFFF
//        if (locks.containsKey(lockInfo.macAddress))
//            return locks[lockInfo.macAddress]

        val newLock = if (getFirmwareModelTraits(lockInfo.model).contains(SunionTraits.WiFi))
            createWifiLock(lockInfo)
        else
            wifiLock.init(lockInfo)
//            BleLock(lockInfo)

        if (newLock == null)
            return null

//        locks[lockInfo.macAddress] = newLock

        return newLock
    }

    private suspend fun createWifiLock(lockInfo: LockInfo): WifiLock? {
        if (lockInfo.serialNumber == null)
            return null

        val wifiLock = wifiLock.init(lockInfo)

        provisionDomain.provisionCreate(
                serialNumber = lockInfo.serialNumber,
                deviceName = lockInfo.deviceName,
                timeZone = ZoneId.systemDefault().id,
                timeZoneOffset = TimeZone.getDefault()
                    .getOffset(System.currentTimeMillis()) / 1000,
                clientToken = getUuid.invoke(),
                model = lockInfo.model
        )


        return wifiLock
    }
}