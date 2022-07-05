package com.sunion.ikeyconnect.domain.blelock

import com.polidea.rxandroidble2.*
import com.polidea.rxandroidble2.scan.ScanSettings
import com.sunion.ikeyconnect.domain.Interface.SunionWifiService
import com.sunion.ikeyconnect.domain.Interface.WifiListResult
import com.sunion.ikeyconnect.domain.command.ConnectWifiCommand
import com.sunion.ikeyconnect.domain.command.WifiConnectState
import com.sunion.ikeyconnect.domain.command.WifiListCommand
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.EventState
import com.sunion.ikeyconnect.domain.model.LockInfo
import com.sunion.ikeyconnect.domain.toHex
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactiveStatefulConnection @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val bleHandShakeUseCase: BleHandShakeUseCase,
    private val mBleCmdRepository: BleCmdRepository,
    private val wifiListCommand: WifiListCommand,
    private val connectWifiCommand: ConnectWifiCommand,
) : StatefulConnection, SunionWifiService {

    companion object {
        val NOTIFICATION_CHARACTERISTIC = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
    }

    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectWifiState = mutableListOf<String>()
    private val _connectionState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connectionState: SharedFlow<Event<Pair<Boolean, String>>> = _connectionState
    private val _connectionState2 = MutableSharedFlow<BluetoothConnectState>()
    val connectionState2: SharedFlow<BluetoothConnectState> = _connectionState2
    private var _connection: Observable<RxBleConnection>? = null
//    private val connectionTimer = CountDownTimer(30000, 1000)

    fun device(input: String): RxBleDevice {
        return rxBleClient.getBleDevice(input)
    }

    override val trashBin: CompositeDisposable = CompositeDisposable()
    override var macAddress: String? = null
    override var connectionDisposable: Disposable? = null
    override val disconnectTriggerSubject = PublishSubject.create<Boolean>()
    private var _rxBleConnection: RxBleConnection? = null
    private var _bleDevice: RxBleDevice? = null
    var keyTwo = ""
    override var connection: Observable<RxBleConnection>
        get() {
            return _connection ?: connectionFallback()
        }
        set(value) {
//            connectionTimer.onClear()
            this._connection = value
        }
    private var connectionTimerJob: Job? = null
    private var scanJob: Job? = null
    private var connectionJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lastConnectionState = MutableStateFlow<Event<Pair<Boolean, String>>?>(null)

    override fun connectionFallback(): Observable<RxBleConnection> {
        TODO("Not yet implemented")
    }

    override fun isConnectedWithDevice(): Boolean {
        TODO("Not yet implemented")
    }

    fun connect_asflow(lockInfo: LockInfo) {

        //todo 30sec timeout
        connectionTimerJob = coroutineScope.launch {
            Timber.d("coroutineScope 30 sec")
            delay(30000)
            connectionDisposable?.dispose()
            _bleDevice = null
            scanJob?.cancel()
            emitConnectionState(Event.error(TimeoutException::class.java.simpleName))
        }
//        rxBleClient.getBleDevice(macAddress)
//            .let {
//                observeConnectionStateChanges(it)
//                connectToDevice(it, macAddress)
//            }

        scanJob?.cancel()
        scanJob =
            flow { emit( rxBleClient.getBleDevice(lockInfo.macAddress))}
                .onEach{
                    _bleDevice = it
                    observeConnectionStateChanges(it)
                    connectToDevice(it, lockInfo.macAddress)
                    scanJob?.cancel()
                }
                .catch {
                    Timber.e(it)
                }
                .launchIn(lockScope)
    }

    fun connectToDevice(rxBleDevice: RxBleDevice, macAddress: String) {
        scanJob?.cancel()
        connectionJob?.cancel()

//        connectionTimerJob = coroutineScope.launch {
//            Timber.d("coroutineScope 30 sec")
//            delay(30000)
//            connectionDisposable?.dispose()
//            _bleDevice = null
////            lockScope.cancel()
//            emitConnectionState(Event.error(TimeoutException::class.java.simpleName))
//        }

        connectionJob = rxBleDevice
            .establishConnection(false)
            .doOnSubscribe { emitConnectionState(Event.loading()) } //too late
            .asFlow()
            .flatMapConcat {
                _rxBleConnection = it
                it.requestMtu(RxBleConnection.GATT_MTU_MAXIMUM).toObservable().asFlow()
            }
            .flatMapConcat {
                val info = bleHandShakeUseCase.getLockConnection(macAddress).blockingGet()
                bleHandShakeUseCase.invoke(
                    input1 = info,
                    deviceMacAddress = macAddress,
                    deviceName = device(macAddress).name,
                    input3 = _rxBleConnection!!
                ).asFlow()
            }
            .flatMapConcat { permission ->
//                Timber.d("permission:$permission")
                connectionTimerJob?.cancel() //avoid to be disconnected bluetooth.
                emitConnectionState((Event.success(Pair(true, permission))))
//                delay(1000)
                bleHandShakeUseCase.getLockConnection(macAddress).toObservable().asFlow()
            }
//            .onStart { emitLoading() }
//            .retry(3) {
//                delay(2000)
//                true
//            }
            .onEach { info ->
                _bleDevice = rxBleDevice
                keyTwo = info.keyTwo
//                useWifi = info.useWifi == true
//                emitSuccess()
            }
            .catch {
//                Timber.tag("WifiLock.connectToDevice").e(it)
                _bleDevice = null
                emitConnectionState(Event.error(TimeoutException::class.java.simpleName))
//                emitWifiListState((WiFiListUiState()))
            }
            .launchIn(lockScope)
    }



    private fun emitConnectionState(event: Event<Pair<Boolean, String>>) {
        runBlocking {
//            delay(5000)
            _connectionState.emit(event)
            lastConnectionState.value = event
        }
    }


    private fun emitWifiListState(event: Event<Pair<Boolean, String>>) {
        runBlocking {
            _connectionState.emit(event)
            lastConnectionState.value = event
        }
    }

    private fun observeConnectionStateChanges(rxBleDevice: RxBleDevice) {
        rxBleDevice
            .observeConnectionStateChanges()
            .asFlow()
            .onStart { emitLoading() }
            .map {
                when (it) {
                    RxBleConnection.RxBleConnectionState.CONNECTING -> emitLoading()
                    RxBleConnection.RxBleConnectionState.CONNECTED -> if (keyTwo.isNotEmpty()) emitSuccess()
                    RxBleConnection.RxBleConnectionState.DISCONNECTED -> emitDisconnect()
                    RxBleConnection.RxBleConnectionState.DISCONNECTING -> emitLoading()
                    null -> emitLoading()
                }
            }
            .catch {
                Timber.e(it)
                emitDisconnect()
            }
            .launchIn(lockScope)
    }

    private fun emitSuccess() {
        lockScope.launch {
            _connectionState.emit(
                Event(status = EventState.SUCCESS, data = Pair(true, ""), message = null)
            )
            _connectionState2.emit(BluetoothConnectState.CONNECTED)
        }
    }

    private fun emitLoading() {
        lockScope.launch {
            _connectionState.emit(
                Event(status = EventState.LOADING, data = null, message = null)
            )
            _connectionState2.emit(BluetoothConnectState.CONNECTING)
        }
    }


    private fun emitDisconnect() {
        _bleDevice = null
        lockScope.launch {
            _connectionState.emit(
                Event(status = EventState.ERROR, data = Pair(false, ""), message = null)
            )
            _connectionState2.emit(BluetoothConnectState.DISCONNECTED)
        }
    }

    fun updateBleDeviceState() {
//        bleDeviceStateUseCase
//            .invoke(null)
//            .subscribeOn(scheduler.single())
//            .subscribe(
//                { locks -> _bleDevice.postValue(Event.success(locks)) },
//                { _bleDevice.postValue(Event.error(it::class.java.simpleName)) }
//            )
//            .apply { trashBin.add(this) }
    }

    override fun actionAfterConnectionError(
        error: Throwable,
        macAddress: String,
        isSilentlyFail: Boolean
    ) {
        Timber.e("connection error called: $error")

        connectionTimerJob?.cancel()

//        _errorOccupiedCheck.postValue(Event.success(macAddress))

        if (isSilentlyFail) {
            emitConnectionState(Event.error(error::class.java.simpleName))
        } else {
            emitConnectionState(Event.error(error::class.java.simpleName, false to macAddress))
        }
        this.macAddress = null
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
        this.macAddress = null
        emitConnectionState(Event.success(Pair(false, "")))
        disconnectTriggerSubject.onNext(true)
        connectionDisposable?.dispose()
        connectionDisposable = null
        _connection = null
        trashBin.clear()
        coroutineScope.cancel()
    }


    override fun collectWifiList(lockInfo: LockInfo): Flow<WifiListResult> = run {
        _rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
            .flatMap { it }
            .asFlow()
            .filter { wifiListCommand.match(lockInfo.keyTwo, it) }
            .map { notification ->
                Timber.d(notification.toHex())
                val result = wifiListCommand.parseResult(lockInfo.keyTwo, notification)
                Timber.d("cmdResponse:$result")
                result
            }
    }

    override suspend fun scanWifi(lockInfo: LockInfo) {
        val command = wifiListCommand.create(lockInfo.keyTwo, Unit)
        if ( _rxBleConnection == null) return
        _rxBleConnection!!
            .writeCharacteristic(NOTIFICATION_CHARACTERISTIC, command).toObservable().asFlow()
            .flowOn(Dispatchers.IO)
            .onEach { Timber.d("OK") }
            .catch { Timber.e(it) }
            .launchIn(lockScope)
    }

    override fun collectConnectToWifiState(lockInfo: LockInfo): Flow<WifiConnectState> =
        _rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .filter { connectWifiCommand.match(lockInfo.keyTwo, it) }
            .map { notification -> connectWifiCommand.parseResult(lockInfo.keyTwo, notification) }

    override suspend fun connectLockToWifi(ssid: String, password: String, lockInfo: LockInfo): Boolean {
        val connection = _rxBleConnection ?: return false
        val command_set_ssid = wifiListCommand.setSSID(lockInfo.keyTwo, ssid)
        val command_set_password = wifiListCommand.setPassword(lockInfo.keyTwo, password)
        val command_connect = wifiListCommand.connect(lockInfo.keyTwo)
        connection
            .writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC, command_set_ssid
            )
            .flatMap {
                connection
                    .writeCharacteristic(
                        NOTIFICATION_CHARACTERISTIC,
                        command_set_password
                    )
            }
            .flatMap {
                connection
                    .writeCharacteristic(NOTIFICATION_CHARACTERISTIC, command_connect)
            }
            .toObservable()
            .asFlow()
            .onCompletion { connectWifiState.clear() }
            .single()
        return true
    }
    fun getIsConnected(): Boolean{
        return _bleDevice?.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED
    }
}