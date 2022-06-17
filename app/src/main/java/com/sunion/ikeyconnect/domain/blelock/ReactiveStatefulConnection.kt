package com.sunion.ikeyconnect.domain.blelock

import android.annotation.SuppressLint
import android.util.Log
import android.util.Base64
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.*
import com.polidea.rxandroidble2.scan.ScanSettings
import com.sunion.ikeyconnect.domain.Interface.SunionWifiService
import com.sunion.ikeyconnect.domain.Interface.WifiListResult
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.toHex
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase
import com.sunion.ikeyconnect.lock.WifiConnectState
import com.sunion.ikeyconnect.unless
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactiveStatefulConnection @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val bleHandShakeUseCase: BleHandShakeUseCase,
    private val mBleCmdRepository: BleCmdRepository,
    private val wifiListCommand: WifiListCommand,
) : StatefulConnection, SunionWifiService {

    companion object {
        val NOTIFICATION_CHARACTERISTIC = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
    }

    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectWifiState = mutableListOf<String>()
    private val _connectionState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connectionState: SharedFlow<Event<Pair<Boolean, String>>> = _connectionState
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
     fun connect_asflow(macAddress: String) {
        rxBleClient.getBleDevice(macAddress)
            .let {
                _bleDevice = it
//                observeConnectionStateChanges(it)
                connectToDevice(it, macAddress)
            }
    }

    fun connectToDevice(rxBleDevice: RxBleDevice, macAddress: String) {
        scanJob?.cancel()
        connectionJob?.cancel()
        connectionJob = rxBleDevice
            .establishConnection(false)
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
                Timber.d("permission:$permission")
                emitConnectionState((Event.success(Pair(true, permission))))
                delay(2000)
                bleHandShakeUseCase.getLockConnection(macAddress).toObservable().asFlow()
            }
//            .onStart { emitLoading() }
            .retry(3) {
                delay(2000)
                true
            }
            .onEach { info ->
                keyTwo = info.keyTwo
//                useWifi = info.useWifi == true
//                emitSuccess()
            }
            .catch {
                Timber.tag("WifiLock.connectToDevice").e(it)
//                emitDisconnect()
            }
            .launchIn(lockScope)
    }
    override fun establishConnection(macAddress: String, isSilentlyFail: Boolean): Disposable {
        close()

        connectionTimerJob = coroutineScope.launch {
            Timber.d("coroutineScope 30 sec")
            delay(30000)
            connectionDisposable?.dispose()
            emitConnectionState(Event.error(TimeoutException::class.java.simpleName))
        }

        this.macAddress = macAddress.uppercase(Locale.ROOT)
        val device = rxBleClient.getBleDevice(macAddress.uppercase(Locale.ROOT))
        _bleDevice = device
//        val disposable = rxBleClient.scanBleDevices().subscribe({
//            if(it.bleDevice.name == "BT_Lock_7B126A")
//                Log.d("TAG",it.bleDevice.macAddress + " : " +it.bleDevice.name)
//        })
        // please see https://github.com/Polidea/RxAndroidBle/wiki/Tutorial:-Connection-Observable-sharing
        val connection = establishBleConnectionAndRequestMtu(device)
            .doOnSubscribe { emitConnectionState(Event.loading()) }
            .compose(ReplayingShare.instance())


        val disposable = runConnectionSequence(connection, device, isSilentlyFail)
        connectionDisposable = disposable

        return disposable
    }

    fun establishConnection_by_deviceName(deviceName: String, isSilentlyFail: Boolean): Disposable {
        close()

        connectionTimerJob = coroutineScope.launch {
            Timber.d("coroutineScope 30 sec")
            delay(30000)
            connectionDisposable?.dispose()
            emitConnectionState(Event.error(TimeoutException::class.java.simpleName))
        }

        val disposable =
            rxBleClient.scanBleDevices(
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build()
            )
                .filter { it.bleDevice.name.equals(deviceName, ignoreCase = true) }
                .take(1)
                .map { it.bleDevice }
                .subscribe { device ->
                    _bleDevice = device
                    Log.d("TAG", device.macAddress + " : " + device.name)
                    val connect = establishBleConnectionAndRequestMtu(device)
                    runConnectionSequence(connect, device, isSilentlyFail)
                }

        connectionDisposable = disposable
        return disposable
    }

    private fun emitConnectionState(event: Event<Pair<Boolean, String>>) {
        runBlocking {
            _connectionState.emit(event)
            lastConnectionState.value = event
        }
    }

    override fun establishBleConnectionAndRequestMtu(device: RxBleDevice): Observable<RxBleConnection> {
        return Observable.timer(500, TimeUnit.MILLISECONDS)
            .flatMap { device.establishConnection(false) }
            .takeUntil(
                disconnectTriggerSubject.doOnNext { isDisconnected ->
                    unless(isDisconnected) {
                        emitConnectionState(Event.success(Pair(false, "")))
                    }
                }
            )
            .flatMap { connection ->
                Timber.d("connected to device and request mtu :${connection.mtu}")
                connection
                    .requestMtu(RxBleConnection.GATT_MTU_MAXIMUM)
                    .ignoreElement()
                    .doOnError { error -> Timber.d(error) }
                    .andThen(Observable.just(connection))
            }
    }

    override fun runConnectionSequence(
        rxBleConnection: Observable<RxBleConnection>,
        device: RxBleDevice,
        isSilentlyFail: Boolean
    ): Disposable {
        return connectionSequenceObservable(device, rxBleConnection)
            .doOnSubscribe { emitConnectionState(Event.loading()) }
            .doOnNext {
                this._connection = rxBleConnection
            }
            .subscribeOn(Schedulers.single())
            .subscribe(
                { permission -> actionAfterDeviceTokenExchanged(permission, device) },
                { actionAfterConnectionError(it, device.macAddress, isSilentlyFail) }
            )
            .apply { trashBin.add(this) }
    }

    fun connectionSequenceObservable(
        device: RxBleDevice,
        rxBleConnection: Observable<RxBleConnection>
    ): Observable<String> {
        val bluetoothGattRefreshCustomOp = refreshAndroidStackCacheCustomOperation()
        val discoverServicesCustomOp = customDiscoverServicesOperation()

        return rxBleConnection
            .flatMap { rxConnection ->
                Timber.d("device mtu size: ${rxConnection.mtu}")
                _rxBleConnection = rxConnection
                rxConnection
                    .queue(bluetoothGattRefreshCustomOp)
                    .ignoreElements()
                    .andThen(rxConnection.queue(discoverServicesCustomOp))
                    .flatMapSingle { bleHandShakeUseCase.getLockConnection(device.macAddress) }
                    .flatMap { connection ->
                        bleHandShakeUseCase.invoke(
                            connection,
                            device.macAddress,
                            device.name,
                            rxConnection
                        )
                    }
            }
    }

    private fun refreshAndroidStackCacheCustomOperation() =
        RxBleCustomOperation { bluetoothGatt, _, _ ->
            try {
                val bluetoothGattRefreshFunction: Method =
                    bluetoothGatt.javaClass.getMethod("refresh")
                val success = bluetoothGattRefreshFunction.invoke(bluetoothGatt) as Boolean
                if (!success) {
                    Observable.error(RuntimeException("BluetoothGatt.refresh() returned false"))
                } else {
                    Observable.empty<Void>().delay(200, TimeUnit.MILLISECONDS)
                }
            } catch (e: NoSuchMethodException) {
                Observable.error<Void>(e)
            } catch (e: IllegalAccessException) {
                Observable.error<Void>(e)
            } catch (e: InvocationTargetException) {
                Observable.error<Void>(e)
            }
        }

    @SuppressLint("MissingPermission")
    private fun customDiscoverServicesOperation() =
        RxBleCustomOperation { bluetoothGatt, rxBleGattCallback, _ ->
            val success: Boolean = bluetoothGatt.discoverServices()
            if (!success) {
                Observable.error(RuntimeException("BluetoothGatt.discoverServices() returned false"))
            } else {
                rxBleGattCallback.onServicesDiscovered
                    .take(1) // so this RxBleCustomOperation will complete after the first result from BluetoothGattCallback.onServicesDiscovered()
                    .map(RxBleDeviceServices::getBluetoothGattServices)
            }
        }

    override fun actionAfterDeviceTokenExchanged(permission: String, device: RxBleDevice) {
        unless(permission.isNotBlank()) {
            connectionTimerJob?.cancel()
            Timber.d("action after device token exchanged: connected with device: ${device.macAddress}, and the stateful connection has been shared: $connection")
//            preferenceStore.connectedMacAddress = device.macAddress
            // after token exchange, update successfully, and connection object had been shared,
            // the E5, D6 notification will emit to downstream
            // the E5 had been intercepted and had been stored in Room database,
            // we then handle the D6 lock's current status and publish to those observers.
            emitConnectionState((Event.success(Pair(true, permission))))
            // populate lock list
            updateBleDeviceState()
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


    override fun collectWifiList(): Flow<WifiListResult> = run {
        _rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
            .flatMap { it }
            .asFlow()
            .filter {
                Timber.d(it.toHex())
                mBleCmdRepository.decrypt(Base64.decode(keyTwo, Base64.DEFAULT), it)?.component3()
                    ?.unSignedInt() == 0xF0

//                wifiListCommand.match(keyTwo, it)
            }
            .map {
                Timber.d(it.toHex())
                wifiListCommand.parseResult(keyTwo, it)
            }
            .map { cmdResponse ->
                Timber.d("cmdResponse:$cmdResponse")
                if (cmdResponse == "LE")
                    WifiListResult.End
                else
                    WifiListResult.Wifi(cmdResponse, true)
            }
    }

    override suspend fun scanWifi() {
        val command = wifiListCommand.create(keyTwo)
        _rxBleConnection!!
            .writeCharacteristic(NOTIFICATION_CHARACTERISTIC, command).toObservable().asFlow()
            .flowOn(Dispatchers.IO)
            .onEach { written: ByteArray ->
                Timber.d("[F0] has written: ${written.toHex()}")
                mBleCmdRepository.resolveF0(Base64.decode(keyTwo, Base64.DEFAULT), written)
                Timber.d("OK") }
            .catch { Timber.e(it) }
            .launchIn(lockScope)
    }

    override fun collectConnectToWifiState(): Flow<WifiConnectState> =
        _rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .onStart { connectWifiState.clear() }
            .filter { String(byteArrayOf(it[0])) == SunionWifiService.CMD_CONNECT }
            .map { String(it) }
            .map { cmdResponse ->
                Timber.d(cmdResponse)
                connectWifiState.add(cmdResponse)
                WifiConnectState(
                    isWifiConnected = connectWifiState.contains("CWiFi Succ"),
                    isIotConnected = connectWifiState.contains("CMQTT Succ")
                )
            }

    override suspend fun connectLockToWifi(ssid: String, password: String): Boolean {
        val connection = _rxBleConnection ?: return false
        connection
            .writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC, ("${SunionWifiService.CMD_SET_SSID_PREFIX}$ssid").toByteArray()
            )
            .flatMap {
                connection
                    .writeCharacteristic(
                        NOTIFICATION_CHARACTERISTIC,
                        ("${SunionWifiService.CMD_SET_PASSWORD_PREFIX}$password").toByteArray()
                    )
            }
            .flatMap {
                connection
                    .writeCharacteristic(NOTIFICATION_CHARACTERISTIC, SunionWifiService.CMD_CONNECT.toByteArray())
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