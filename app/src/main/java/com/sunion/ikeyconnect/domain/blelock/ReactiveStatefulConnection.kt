package com.sunion.ikeyconnect.domain.blelock

import android.util.Base64
import com.polidea.rxandroidble2.*
import com.sunion.ikeyconnect.domain.Interface.SunionWifiService
import com.sunion.ikeyconnect.domain.Interface.WifiListResult
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository.Companion.NOTIFICATION_CHARACTERISTIC
import com.sunion.ikeyconnect.domain.command.ConnectWifiCommand
import com.sunion.ikeyconnect.domain.command.GetLockSettingCommand
import com.sunion.ikeyconnect.domain.command.WifiConnectState
import com.sunion.ikeyconnect.domain.command.WifiListCommand
import com.sunion.ikeyconnect.domain.exception.NotConnectedException
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.EventState
import com.sunion.ikeyconnect.domain.model.LockInfo
import com.sunion.ikeyconnect.domain.model.LockSetting
import com.sunion.ikeyconnect.domain.toHex
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
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

    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectWifiState = mutableListOf<String>()
    private val _connectionState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connectionState: SharedFlow<Event<Pair<Boolean, String>>> = _connectionState
    private val _bleLockState = MutableSharedFlow<LockSetting>()
    val bleLockState: SharedFlow<LockSetting> = _bleLockState
    private val _connectionState2 = MutableSharedFlow<BluetoothConnectState>()
    val connectionState2: SharedFlow<BluetoothConnectState> = _connectionState2
    private var _connection: Observable<RxBleConnection>? = null

    fun device(input: String): RxBleDevice {
        return rxBleClient.getBleDevice(input)
    }

    override val trashBin: CompositeDisposable = CompositeDisposable()
    override var macAddress: String? = null
    override var connectionDisposable: Disposable? = null
    override val disconnectTriggerSubject = PublishSubject.create<Boolean>()
    var _rxBleConnection: RxBleConnection? = null
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
        return _connection ?: Observable.error(NotConnectedException())
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

    private fun connectToDevice(rxBleDevice: RxBleDevice, macAddress: String) {
        scanJob?.cancel()
        connectionJob?.cancel()

        connectionTimerJob = coroutineScope.launch {
            Timber.d("coroutineScope 15 sec")
            delay(15000)
            connectionDisposable?.dispose()
            _bleDevice = null
//            lockScope.cancel()
            emitConnectionState(Event.error(TimeoutException::class.java.simpleName))
        }

         connectionJob = rxBleDevice
            .establishConnection(false)
            .doOnSubscribe { emitConnectionState(Event.loading()) } //too late
            .asFlow()
            .flatMapConcat {
                connectionTimerJob?.cancel() //avoid to disconnect bluetooth.
                _rxBleConnection = it
                it.requestMtu(RxBleConnection.GATT_MTU_MAXIMUM).toObservable().asFlow()
            }
            .flatMapConcat {
                bleHandShakeUseCase.getLockConnection(macAddress).toObservable().asFlow()
            }
             .flatMapConcat { info ->
                bleHandShakeUseCase.invoke(
                    input1 = info,
                    deviceMacAddress = macAddress,
                    deviceName = device(macAddress).name,
                    input3 = _rxBleConnection!!
                ).asFlow()
            }
            .filter { permission -> permission.isNotBlank()}
             .flatMapConcat { permission ->
                 Timber.d("connect get permission = $permission")
                 emitConnectionState((Event.success(Pair(true, permission))))
                 collectLockSetting()
                 bleHandShakeUseCase.getLockConnection(macAddress).toObservable().asFlow()
             }
             .map { info ->
                _bleDevice = rxBleDevice
                keyTwo = info.keyTwo
             }
             .catch {
                _bleDevice = null
                emitConnectionState(Event.error(TimeoutException::class.java.simpleName))
             }
             .flowOn(Dispatchers.IO)
             .launchIn(lockScope)
    }

    fun emitConnectStateAfterPairing(){
        emitConnectionState((Event.success(Pair(true, "A"))))
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
        return Observable.just(_rxBleConnection).flatMap { rxConnection ->
            Observable.zip(
                rxConnection.setupNotification(
                    NOTIFICATION_CHARACTERISTIC,
                    NotificationSetupMode.DEFAULT
                )
                    .flatMap { notification -> notification },
                rxConnection.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, bytes).toObservable(),
                BiFunction { notification: ByteArray, _: ByteArray ->
                    notification
                }
            )
        }
    }

    override fun sendCommandThenWaitNotifications(bytes: ByteArray): Observable<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun setupNotificationsFor(function: Int): Observable<ByteArray> {
        return Observable.just(_rxBleConnection).flatMap { rxConnection ->
            rxConnection.setupNotification(NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
                .flatMap { it }
        }
    }

    override fun addDisposable(disposable: Disposable): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        this.macAddress = null
        disconnectTriggerSubject.onNext(true)
        connectionDisposable?.dispose()
        connectionDisposable = null
        _connection = null
        _rxBleConnection = null
        emitDisconnect()
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

    private fun collectLockSetting() {
        val command = GetLockSettingCommand(mBleCmdRepository)
        (_rxBleConnection?:throw Exception("_rxBleConnectionNull"))
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .filter { notification -> command.match(keyTwo, notification) }
            .flatMapConcat { notification ->
                Timber.tag("@").i("Nice Connect!")
                flow { emit(command.parseResult(keyTwo, notification)) }}
            .map {
                _bleLockState.emit(it)
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.tag("xxx").e(it) }
            .launchIn(lockScope)
    }

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

    fun setupSingleNotificationThenSendCommand(
        command: ByteArray,
        functionName: String = ""
    ): Flow<ByteArray> {
        Timber.d("--> en:${command.toHex()} by $functionName")
        if (BuildConfig.DEBUG) {
            mBleCmdRepository.decrypt(Base64.decode(keyTwo, Base64.DEFAULT), command)
                ?.let {
                    Timber.d("--> de:${it.toHex()} by $functionName")
                }
        }

        return _rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .onStart {
                lockScope.launch(Dispatchers.IO) {
                    delay(200)
                    _rxBleConnection!!.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, command).toObservable()
                        .asFlow().single()
                }
            }
            .onEach { notification ->
                Timber.d("<-- en:${notification.toHex()} by $functionName")
                if (BuildConfig.DEBUG) {
                    mBleCmdRepository.decrypt(Base64.decode(keyTwo, Base64.DEFAULT), command)
                        ?.let {
                            Timber.d("--> de:${it.toHex()} by $functionName")
                        }
                }
            }
    }
}