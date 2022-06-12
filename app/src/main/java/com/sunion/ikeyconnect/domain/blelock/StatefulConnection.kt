package com.sunion.ikeyconnect.domain.blelock

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.ikeyconnect.domain.model.Event
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

interface StatefulConnection {

    val trashBin: CompositeDisposable

    var macAddress: String?

    var connectionDisposable: Disposable?

    val disconnectTriggerSubject: PublishSubject<Boolean>

//    val errorOccupiedCheck: LiveData<Event<String>>
//
    val connectionState: LiveData<Event<Pair<Boolean, String>>>
//
//    val bleDevice: MutableLiveData<Event<List<BleDevice>>>

    var connection: Observable<RxBleConnection>

    fun connectionFallback(): Observable<RxBleConnection>

    fun isConnectedWithDevice(): Boolean

    fun establishConnection(macAddress: String, isSilentlyFail: Boolean): Disposable

    fun establishBleConnectionAndRequestMtu(device: RxBleDevice): Observable<RxBleConnection>

    fun runConnectionSequence(
        rxBleConnection: Observable<RxBleConnection>,
        device: RxBleDevice,
        isSilentlyFail: Boolean
    ): Disposable

    fun actionAfterDeviceTokenExchanged(permission: String, device: RxBleDevice)

    fun actionAfterConnectionError(
        error: Throwable,
        macAddress: String,
        isSilentlyFail: Boolean
    )

    fun sendBytes(bytes: ByteArray): Observable<ByteArray>

    fun sendCommandThenWaitSingleNotification(bytes: ByteArray): Observable<ByteArray>

    fun sendCommandThenWaitNotifications(bytes: ByteArray): Observable<ByteArray>

    infix fun setupNotificationsFor(function: Int): Observable<ByteArray>

    fun addDisposable(disposable: Disposable): Boolean

    fun close()
}