package com.sunion.ikeyconnect.domain.blelock

import android.util.Base64
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactiveStatefulConnection @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val bleHandShakeUseCase: BleHandShakeUseCase
) : StatefulConnection{

    fun device(input: String): RxBleDevice {
        return rxBleClient.getBleDevice(input)
    }
    fun connectWithToken(mLock: LockConnectionInformation, rxBleConnection: RxBleConnection): Observable<String> {
        val keyOne = Base64.decode(mLock.keyOne, Base64.DEFAULT)
        val token = if (mLock.permanentToken.isBlank()) {
            Base64.decode(mLock.oneTimeToken, Base64.DEFAULT)
        } else {
            Base64.decode(mLock.permanentToken, Base64.DEFAULT)
        }
        val isLockFromSharing =
            mLock.sharedFrom != null && mLock.sharedFrom.isNotBlank()

        return if (mLock.permanentToken.isBlank()) {
            bleHandShakeUseCase.connectWithOneTimeToken(mLock, rxBleConnection, keyOne, token, isLockFromSharing)
        } else {
            bleHandShakeUseCase.connectWithPermanentToken(keyOne, token, mLock, rxBleConnection, isLockFromSharing)
        }
    }



    override val trashBin: CompositeDisposable
        get() = TODO("Not yet implemented")
    override var macAddress: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var connectionDisposable: Disposable?
        get() = TODO("Not yet implemented")
        set(value) {}
    override val disconnectTriggerSubject: PublishSubject<Boolean>
        get() = TODO("Not yet implemented")
    override var connection: Observable<RxBleConnection>
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun connectionFallback(): Observable<RxBleConnection> {
        TODO("Not yet implemented")
    }

    override fun isConnectedWithDevice(): Boolean {
        TODO("Not yet implemented")
    }

    override fun establishConnection(macAddress: String, isSilentlyFail: Boolean): Disposable {
        TODO("Not yet implemented")
    }

    override fun establishBleConnectionAndRequestMtu(device: RxBleDevice): Observable<RxBleConnection> {
        TODO("Not yet implemented")
    }

    override fun runConnectionSequence(
        rxBleConnection: Observable<RxBleConnection>,
        device: RxBleDevice,
        isSilentlyFail: Boolean
    ): Disposable {
        TODO("Not yet implemented")
    }

    override fun actionAfterDeviceTokenExchanged(permission: String, device: RxBleDevice) {
        TODO("Not yet implemented")
    }

    override fun actionAfterConnectionError(
        error: Throwable,
        macAddress: String,
        isSilentlyFail: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun sendBytes(bytes: ByteArray): Observable<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun sendCommandThenWaitSingleNotification(bytes: ByteArray): Observable<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun sendCommandThenWaitNotifications(bytes: ByteArray): Observable<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun setupNotificationsFor(function: Int): Observable<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun addDisposable(disposable: Disposable): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}