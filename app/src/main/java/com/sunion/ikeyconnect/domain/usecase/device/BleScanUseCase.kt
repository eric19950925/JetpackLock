package com.sunion.ikeyconnect.domain.usecase.device

import android.os.PowerManager
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.usecase.UseCase
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleScanUseCase @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val powerManager: PowerManager,
    private val lockInformationRepository: LockInformationRepository
) : UseCase.Execute<Unit, Observable<ScanResult>> {

    override fun invoke(input: Unit): Observable<ScanResult> {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

        return rxBleClient
            .scanBleDevices(scanSettings)
            .filter { it.bleDevice.name?.contains("BT_Lock") ?: false }
    }

    fun longRunningScan(macAddress: String?): Observable<ScanResult> {

        if (macAddress == null) {
            throw IllegalArgumentException("The device's macAddress is null")
        }

        Timber.d("check if device's screen is on: ${powerManager.isInteractive}")

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

        return if (powerManager.isInteractive) {
            Timber.d("Scanning of Lock: $macAddress started, the device screen is on: ${powerManager.isInteractive}")
            rxBleClient
                .scanBleDevices(scanSettings, ScanFilter.empty())
                .filter { it.bleDevice.name?.contains("BT_Lock") ?: false }
        } else {
            Timber.d("Scanning of Lock: $macAddress started, the device screen is on: ${powerManager.isInteractive}")
            lockInformationRepository.get(macAddress)
                .flatMapObservable { lockInfo ->
                    Timber.d("scan device name of ${lockInfo.deviceName} started, macAddress: $macAddress")
                    if (lockInfo.deviceName.isNotBlank()) {
                        rxBleClient
                            .scanBleDevices(ScanSettings.Builder().build(), ScanFilter.Builder().setDeviceName(lockInfo.deviceName).build())
                    } else {
                        rxBleClient
                            .scanBleDevices(ScanSettings.Builder().build(), ScanFilter.Builder().setDeviceAddress(macAddress).build())
                    }
                }
        }
    }
}
