package com.sunion.ikeyconnect.domain.usecase.device

import android.app.PendingIntent
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.polidea.rxandroidble2.NotificationSetupMode
import javax.inject.Inject
import javax.inject.Singleton
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.NotConnectedException
import com.sunion.ikeyconnect.domain.model.DeviceToken
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.toHex
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@Singleton
class BleHandShakeUseCase @Inject constructor(
    private val mBleCmdRepository: BleCmdRepository
){
    companion object {
        const val CIPHER_MODE = "AES/ECB/NoPadding"
        const val BARCODE_KEY = "SoftChefSunion65"
        val NOTIFICATION_CHARACTERISTIC: UUID = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
        val SUNION_SERVICE_UUID: UUID = UUID.fromString("fc3d8cf8-4ddc-7ade-1dd9-2497851131d7")
        const val DATA = "DATA"
        const val CURRENT_LOCK_MAC = "CURRENT_LOCK_MAC"
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        var MY_PENDING_INTENT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    fun connectWithOneTimeToken(
        mLock: LockConnectionInformation,
        rxBleConnection: RxBleConnection,
        keyOne: ByteArray,
        oneTimeToken: ByteArray,
        isLockFromSharing: Boolean
    ): Observable<String> {
        return sendC0(rxBleConnection, keyOne, oneTimeToken)
            .flatMap { keyTwo ->
                Log.d("TAG","get k2")
                sendC1(rxBleConnection, keyTwo, oneTimeToken, isLockFromSharing)
                    .take(1)
                    .filter { it.first == DeviceToken.ONE_TIME_TOKEN || it.first == DeviceToken.VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        rxBleConnection
                            .setupNotification(
                                NOTIFICATION_CHARACTERISTIC,
                                NotificationSetupMode.DEFAULT
                            )
                            .flatMap { it }
                            .filter { notification -> // [E5] will sent from device
                                mBleCmdRepository.decrypt(keyTwo, notification)?.let { bytes ->
                                    bytes.component3().unSignedInt() == 0xE5
                                } ?: false
                            }
                            .distinct { notification ->
                                mBleCmdRepository.decrypt(keyTwo, notification)?.component3()
                                    ?.unSignedInt() ?: 0xE5
                            }
                            .map { notification ->
                                val token =
                                    mBleCmdRepository.decrypt(keyTwo, notification)?.let { bytes ->
                                        val permanentToken = mBleCmdRepository.extractToken(mBleCmdRepository.resolveE5(bytes))
                                        permanentToken
                                    } ?: throw NotConnectedException()
                                keyTwo to token
                            }
                            .doOnNext { pair -> //todo
//                                Timber.d("received receive [C1], [E5], [D6] in exchange one time token")
//                                updateLockInfo(mLock, pair.first, pair.second as DeviceToken.PermanentToken)
                            }
                            .flatMap { pair ->
                                val token = pair.second
                                if (token is DeviceToken.PermanentToken) Observable.just(token.permission) else Observable.never()
                            }
                    }
            }
    }

    fun connectWithPermanentToken(
        keyOne: ByteArray,
        token: ByteArray,
        mLock: LockConnectionInformation,
        rxBleConnection: RxBleConnection,
        isLockFromSharing: Boolean)
            : Observable<String>{
        return  sendC0(rxBleConnection, keyOne, token)
            .flatMap { keyTwo ->
                Log.d("TAG","get k2")
                sendC1(rxBleConnection, keyTwo, token, isLockFromSharing)
                    .filter { it.first == DeviceToken.VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        rxBleConnection
                            .setupNotification(
                                NOTIFICATION_CHARACTERISTIC,
                                NotificationSetupMode.DEFAULT
                            ) // receive [C1], [D6] only
                            .flatMap { it }
                            .map { keyTwo to it }
                            .take(1)
                            .doOnNext { pair ->
//                                Timber.d("received receive [C1], [D6] in exchange permanent token")
//                                viewModelScope.launch { mCharacteristicValue.value = "PermanentToken" to pair }
//                                viewModelScope.launch {repository.LockUpdate(mLock.copy(
//                                    keyTwo = Base64.encodeToString(
//                                        keyTwo,
//                                        Base64.DEFAULT
//                                    ),
//                                    deviceName = mLock.deviceName ,
//                                    permission = stateAndPermission.second
//                                ))}
                            }
                            .flatMap { Observable.just(stateAndPermission.second) }
                    }
            }

    }

    private fun sendC0(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray> {
        return Observable.zip(
            setupC0Notify(rxConnection, keyOne),
            sendC00(rxConnection, keyOne, token),
            BiFunction { notification: ByteArray, written: ByteArray ->
                val randomNumberOne = mBleCmdRepository.resolveC0(keyOne, written)
                Log.d("TAG", "[C0] has written: ${written.toHex()}")
                Log.d("TAG", "[C0] has notified: ${notification.toHex()}")
                val randomNumberTwo = mBleCmdRepository.resolveC0(keyOne, notification)
                Log.d("TAG", "randomNumberTwo: ${randomNumberTwo.toHex()}")
                val keyTwo = mBleCmdRepository.generateKeyTwo(
                    randomNumberOne = randomNumberOne,
                    randomNumberTwo = randomNumberTwo
                )
                Log.d("TAG", "keyTwo: ${keyTwo.toHex()}")
                keyTwo
            })
    }
    private fun sendC1(
        rxConnection: RxBleConnection,
        keyTwo: ByteArray,
        token: ByteArray,
        isLockFromSharing: Boolean
    ): Observable<Pair<Int, String>> {
        return Observable.zip(
            rxConnection.setupNotification(
                NOTIFICATION_CHARACTERISTIC,
                NotificationSetupMode.DEFAULT
            )
                .flatMap { notification -> notification }
                .filter { notification ->
                    mBleCmdRepository.decrypt(keyTwo, notification)?.component3()
                        ?.unSignedInt() == 0xC1
                },
            rxConnection.writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(0xC1, keyTwo, token)
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                //                Timber.d("[C1] has written: ${written.toHex()}")
                //                Timber.d("[C1] has notified: ${notification.toHex()}")
                val tokenStateFromDevice = mBleCmdRepository.resolveC1(keyTwo, notification)
                //                Timber.d("token state from device : ${tokenStateFromDevice.toHex()}")
                val deviceToken = mBleCmdRepository.determineTokenState(tokenStateFromDevice, isLockFromSharing)
                //                Timber.d("token state: ${token.toHex()}")
                val permission = mBleCmdRepository.determineTokenPermission(tokenStateFromDevice)
                //                Timber.d("token permission: $permission")
                deviceToken to permission
            }
        )
    }

    private fun setupC0Notify(
        rxConnection: RxBleConnection,
        keyOne: ByteArray
    ): Observable<ByteArray> {
        return rxConnection.setupNotification(
            NOTIFICATION_CHARACTERISTIC
        ).flatMap {
                notification -> notification
        }.filter { notification ->
            val decrypted = mBleCmdRepository.decrypt(
                keyOne,
                notification
            )
            println("filter [C0] decrypted: ${decrypted?.toHex()}")
            decrypted?.component3()?.unSignedInt() == 0xC0
        }
    }


    private fun sendC00(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray>  {
//    ): Disposable  {
        val writeC0 = mBleCmdRepository.createCommand(0xC0, keyOne, token)
        Log.d("TAG", "writeC0 ${writeC0.toHex()}")
        return rxConnection.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, writeC0)
            .toObservable()
//            .subscribe{
//                Log.d("TAG", "written C0 ${it.toHex()}")
//            }
    }

}