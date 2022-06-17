package com.sunion.ikeyconnect.domain.usecase.device

import android.util.Base64
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.Interface.SunionWifiService
import com.sunion.ikeyconnect.domain.Interface.WifiListResult
import com.sunion.ikeyconnect.domain.Interface.WifiSettingDomain
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.WifiListCommand
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.toHex
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase.Companion.NOTIFICATION_CHARACTERISTIC
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiSettingUseCase @Inject constructor(
    private val mBleCmdRepository: BleCmdRepository,
    private val lockInformationRepository: LockInformationRepository,
    private val wifiListCommand: WifiListCommand,
): WifiSettingDomain {

    override suspend fun searchWifi(
        input1: String,
        input2: RxBleConnection
    ) {
//        val command = wifiListCommand.create(input1)
//        input2
//            .writeCharacteristic(wifiCmdUuid, command).toObservable().asFlow()
//            .flowOn(Dispatchers.IO)
//            .onEach { Timber.d("OK") }
//            .catch { Timber.e(it) }

        input2.writeCharacteristic(
            NOTIFICATION_CHARACTERISTIC,
                    mBleCmdRepository.createCommand(
                        0xF0,
                        Base64.decode(input1, Base64.DEFAULT),
                        "L".toByteArray()
                    )
                )
            .toObservable()
//            .flatMap { written: ByteArray ->
//                mBleCmdRepository.resolveF0(input1, written)
//            }
            .asFlow()
            .flowOn(Dispatchers.IO)
            .onEach { written: ByteArray ->
                mBleCmdRepository.resolveF0(Base64.decode(input1, Base64.DEFAULT), written)
            }
            .catch { Timber.e(it) }
//            .launchIn(lockScope)
    }

    override fun settingSSID() {
        TODO("Not yet implemented")
    }

    override fun settingPassword() {
        TODO("Not yet implemented")
    }

    override fun settingConnection() {
        TODO("Not yet implemented")
    }
    fun F0(
        input1: String,
        rxConnection: RxBleConnection
    ): Observable<String> {
        val k2 = Base64.decode(input1, Base64.DEFAULT)
        return Observable.zip(
            rxConnection.setupNotification(
                NOTIFICATION_CHARACTERISTIC,
                NotificationSetupMode.DEFAULT
            )
                .flatMap { notification -> notification }
                .filter { notification ->
                    val decrypted = mBleCmdRepository.decrypt(
                        k2,
                        notification
                    )
                    println("filter [F0] decrypted: ${decrypted?.toHex()}")
                    decrypted?.component3()?.unSignedInt() == 0xF0
                },
            rxConnection.writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(0xF0, k2, "L".toByteArray())
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                val randomNumberOne = mBleCmdRepository.resolveF0(k2, written)
                Timber.d("[F0] has written: ${written.toHex()}")
                Timber.d("[F0] has notified: ${notification.toHex()}")
                val randomNumberTwo = mBleCmdRepository.resolveF0(k2, notification)
                Timber.d("F0 wifi: ${randomNumberTwo}")
                randomNumberTwo
            }
        )
    }

}