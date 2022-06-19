package com.sunion.ikeyconnect.domain.usecase.device

import android.app.PendingIntent
import android.os.Build
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.polidea.rxandroidble2.NotificationSetupMode
import javax.inject.Inject
import javax.inject.Singleton
import com.polidea.rxandroidble2.RxBleConnection
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.NotConnectedException
import com.sunion.ikeyconnect.domain.model.DeviceToken
import com.sunion.ikeyconnect.domain.model.DeviceToken.DeviceTokenState.VALID_TOKEN
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.toHex
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*

@Singleton
class BleHandShakeUseCase @Inject constructor(
    private val mBleCmdRepository: BleCmdRepository,
    private val lockInformationRepository: LockInformationRepository,
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

    fun getLockConnection(macAddress: String): Single<LockConnectionInformation> {
        return lockInformationRepository.get(macAddress)
    }

    fun invoke(
        input1: LockConnectionInformation,
        deviceMacAddress: String,
        deviceName: String?,
        input3: RxBleConnection
    ): Observable<String> {
        val keyOne = Base64.decode(input1.keyOne, Base64.DEFAULT)
        val isLockFromSharing = input1.sharedFrom != null && input1.sharedFrom?.isNotBlank() ?: false
        return if (input1.permanentToken.isBlank()) {
            Timber.d("exchange one time token")
            val oneTimeToken = Base64.decode(input1.oneTimeToken, Base64.DEFAULT)
            exchangeOneTimeToken(
                deviceMacAddress = deviceMacAddress,
                deviceName = deviceName,
                connection = input3,
                keyOne = keyOne,
                oneTimeToken = oneTimeToken,
                isLockFromSharing = isLockFromSharing
            )
        } else {
            Timber.d("exchange permanent token")
            val permanentToken = Base64.decode(input1.permanentToken, Base64.DEFAULT)
            exchangePermanentToken(
                keyOne = keyOne,
                token = permanentToken,
                deviceMacAddress = deviceMacAddress,
                deviceName = deviceName,
                connection = input3,
                isLockFromSharing = isLockFromSharing
            )
        }
    }

    fun exchangePermanentToken(
        keyOne: ByteArray,
        token: ByteArray,
        deviceMacAddress: String,
        deviceName: String?,
        connection: RxBleConnection,
        isLockFromSharing: Boolean
    ): Observable<String> {
        return sendC0(connection, keyOne, token)
            .flatMap { keyTwo ->
                sendC1(connection, keyTwo, token, isLockFromSharing)
                    .filter { it.first == VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        connection
                            .setupNotification(
                                ReactiveStatefulConnection.NOTIFICATION_CHARACTERISTIC,
                                NotificationSetupMode.DEFAULT
                            ) // receive [C1], [D6] only
                            .flatMap { it }
                            .map { keyTwo to it }
                            .take(1)
                            .doOnNext { _ ->
                                Timber.d("received receive [C1], [D6] in exchange permanent token")
                                lockInformationRepository
                                    .get(deviceMacAddress)
                                    .doOnSuccess { lockConnection ->
                                        saveConnectionInformation(
                                            lockConnection.copy(
                                                keyTwo = Base64.encodeToString(
                                                    keyTwo,
                                                    Base64.DEFAULT
                                                ),
                                                deviceName = deviceName ?: "",
                                                permission = stateAndPermission.second
                                            )
                                        )
                                        Timber.d("update key two successful")
                                    }
                                    .subscribeOn(Schedulers.single())
                                    .subscribe()
                            }
                            .flatMap { Observable.just(stateAndPermission.second) }
                    }
            }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun exchangeOneTimeToken(
        deviceMacAddress: String,
        deviceName: String?,
        connection: RxBleConnection,
        keyOne: ByteArray,
        oneTimeToken: ByteArray,
        isLockFromSharing: Boolean
    ): Observable<String> {
        return sendC0(connection, keyOne, oneTimeToken)
            .flatMap { keyTwo ->
                sendC1(connection, keyTwo, oneTimeToken, isLockFromSharing)
                    .take(1)
                    .filter { it.first == DeviceToken.ONE_TIME_TOKEN || it.first == DeviceToken.VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        connection
                            .setupNotification(
                                ReactiveStatefulConnection.NOTIFICATION_CHARACTERISTIC,
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
                            .doOnNext { pair ->
                                Timber.d("received receive [C1], [E5], [D6] in exchange one time token")
                                val token = pair.second
                                if (token is DeviceToken.PermanentToken) {
                                    lockInformationRepository
                                        .get(deviceMacAddress)
                                        .doOnSuccess { lockConnection ->
                                            saveConnectionInformation(
                                                LockConnectionInformation(
                                                    macAddress = deviceMacAddress,
                                                    displayName = lockConnection.displayName,
                                                    deviceName = deviceName ?: "",
                                                    keyOne = Base64.encodeToString(
                                                        keyOne,
                                                        Base64.DEFAULT
                                                    ),
                                                    keyTwo = Base64.encodeToString(
                                                        pair.first,
                                                        Base64.DEFAULT
                                                    ),
                                                    oneTimeToken = Base64.encodeToString(
                                                        oneTimeToken,
                                                        Base64.DEFAULT
                                                    ),
                                                    permanentToken = token.token,
                                                    isOwnerToken = token.isOwner,
                                                    tokenName = token.name,
                                                    permission = token.permission,
                                                    index = lockConnection.index,
                                                    model = lockConnection.model
                                                )
                                            )
                                            Timber.d("update key two successful")
                                        }
                                        .subscribeOn(Schedulers.single())
                                        .subscribe()
                                }
                            }
                            .flatMap { pair ->
                                val token = pair.second
                                if (token is DeviceToken.PermanentToken) Observable.just(token.permission) else Observable.never()
                            }
                    }
            }
    }

    fun sendC0(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray> {
        return Observable.zip(
            rxConnection.setupNotification(
                ReactiveStatefulConnection.NOTIFICATION_CHARACTERISTIC,
                NotificationSetupMode.DEFAULT
            )
                .flatMap { notification -> notification }
                .filter { notification ->
                    val decrypted = mBleCmdRepository.decrypt(
                        keyOne,
                        notification
                    )
                    println("filter [C0] decrypted: ${decrypted?.toHex()}")
                    decrypted?.component3()?.unSignedInt() == 0xC0
                },
            rxConnection.writeCharacteristic(
                ReactiveStatefulConnection.NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(0xC0, keyOne, token)
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                val randomNumberOne = mBleCmdRepository.resolveC0(keyOne, written)
                Timber.d("[C0] has written: ${written.toHex()}")
                Timber.d("[C0] has notified: ${notification.toHex()}")
                val randomNumberTwo = mBleCmdRepository.resolveC0(keyOne, notification)
                Timber.d("randomNumberTwo: ${randomNumberTwo.toHex()}")
                val keyTwo = generateKeyTwo(
                    randomNumberOne = randomNumberOne,
                    randomNumberTwo = randomNumberTwo
                )
                Timber.d("keyTwo: ${keyTwo.toHex()}")
                keyTwo
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun generateKeyTwo(randomNumberOne: ByteArray, randomNumberTwo: ByteArray): ByteArray {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        return keyTwo
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun sendC1(
        rxConnection: RxBleConnection,
        keyTwo: ByteArray,
        token: ByteArray,
        isLockFromSharing: Boolean
    ): Observable<Pair<Int, String>> {
        return Observable.zip(
            rxConnection.setupNotification(
                ReactiveStatefulConnection.NOTIFICATION_CHARACTERISTIC,
                NotificationSetupMode.DEFAULT
            )
                .flatMap { notification -> notification }
                .filter { notification ->
                    mBleCmdRepository.decrypt(keyTwo, notification)?.component3()
                        ?.unSignedInt() == 0xC1
                },
            rxConnection.writeCharacteristic(
                ReactiveStatefulConnection.NOTIFICATION_CHARACTERISTIC,
                mBleCmdRepository.createCommand(0xC1, keyTwo, token)
            ).toObservable(),
            BiFunction { notification: ByteArray, written: ByteArray ->
                Timber.d("[C1] has written: ${written.toHex()}")
                Timber.d("[C1] has notified: ${notification.toHex()}")
                val tokenStateFromDevice = mBleCmdRepository.resolveC1(keyTwo, notification)
                Timber.d("token state from device : ${tokenStateFromDevice.toHex()}")
                val deviceToken = mBleCmdRepository.determineTokenState(tokenStateFromDevice, isLockFromSharing)
                Timber.d("token state: ${token.toHex()}")
                val permission = mBleCmdRepository.determineTokenPermission(tokenStateFromDevice)
                Timber.d("token permission: $permission")
                deviceToken to permission
            }
        )
    }
    fun saveConnectionInformation(information: LockConnectionInformation) {
        Timber.d("Save connection information: $information")
        lockInformationRepository.save(information)
    }
}