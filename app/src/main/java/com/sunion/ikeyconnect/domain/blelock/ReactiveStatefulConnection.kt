package com.sunion.ikeyconnect.domain.blelock

import android.os.CountDownTimer
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.ikeyconnect.domain.exception.EmptyLockInfoException
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactiveStatefulConnection @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val bleHandShakeUseCase: BleHandShakeUseCase
) : StatefulConnection{
    private val _connectionState = MutableLiveData<Event<Pair<Boolean, String>>>()
    override val connectionState: LiveData<Event<Pair<Boolean, String>>>
        get() = _connectionState
    private var _connection: Observable<RxBleConnection>? = null
//    private val connectionTimer = CountDownTimer(30000, 1000)

    fun device(input: String): RxBleDevice {
        return rxBleClient.getBleDevice(input)
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
        get() {
            return _connection ?: connectionFallback()
        }
        set(value) {
//            connectionTimer.onClear()
            this._connection = value
        }

    override fun connectionFallback(): Observable<RxBleConnection> {
        TODO("Not yet implemented")
    }

    override fun isConnectedWithDevice(): Boolean {
        TODO("Not yet implemented")
    }

    override fun establishConnection(macAddress: String, isSilentlyFail: Boolean): Disposable {
        val d1 = bleHandShakeUseCase.getLockConnection(macAddress).toObservable()
        val d2 = device(macAddress)
                .establishConnection(false)
        val disposable =
            Observable.zip(
                d1, d2, BiFunction { mLock: LockConnectionInformation, rxBleConnect: RxBleConnection ->
                    bleHandShakeUseCase.connectWithToken(mLock, rxBleConnect)
                }
            )
            .subscribe(
                {
                println("Receive: $it")

                },{ it ->
                    Timber.e(it.toString())
                })
        return disposable
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