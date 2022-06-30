package com.sunion.ikeyconnect.lock

import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.ikeyconnect.domain.Interface.*
import com.sunion.ikeyconnect.domain.blelock.BluetoothConnectState
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.command.WifiConnectState
import com.sunion.ikeyconnect.domain.exception.EmptyLockInfoException
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiLock @Inject constructor(
    private val lockInformationRepository: LockInformationRepository,
    private val userCodeRepository: UserCodeRepository,
    private val eventLogRepository: LockEventLogRepository,
    private val statefulConnection: ReactiveStatefulConnection,
    private val iotService: SunionIotService,
    private val getIdToken: GetIdTokenUseCase,
    ) : Lock {

    private var connectionDisposable: Disposable? = null

    private var mLock: LockConnectionInformation? = null

    private var _lockInfo: LockInfo? = null
    override val lockInfo: LockInfo
        get() = _lockInfo ?: throw EmptyLockInfoException()

    private var bleDevice: RxBleDevice? = null
    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var rxBleConnection: RxBleConnection? = null
    private var scanJob: Job? = null
    private var connectionJob: Job? = null
    private var keyTwo = ""
    private val wifiCmdUuid = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
    private val connectWifiState = mutableListOf<String>()
    private val _connectionState2 = MutableSharedFlow<BluetoothConnectState>()
    val connectionState2: SharedFlow<BluetoothConnectState> = statefulConnection.connectionState2

//    private val _connectionState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connectionState: SharedFlow<Event<Pair<Boolean, String>>> = statefulConnection.connectionState

    fun init(lockInfo: LockInfo): WifiLock {
        this._lockInfo = lockInfo
        return this
    }

    override fun connect() {
        statefulConnection.connect_asflow(lockInfo)
//        statefulConnection.establishConnection(lockInfo.macAddress, false)
        Timber.d("find ${lockInfo.deviceName}")
//        statefulConnection.establishConnection_by_deviceName(lockInfo.deviceName, false)
        }

    override fun disconnect() {
        connectionDisposable?.dispose()
        statefulConnection.close()
    }

    override fun isConnected(): Boolean = statefulConnection.getIsConnected()

    override fun getName(shouldSave: Boolean): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun setTime(epochSecond: Long): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun getLockName(): String {
        TODO("Not yet implemented")
    }

    override suspend fun hasAdminCodeBeenSet(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun changeLockName(name: String, clientToken: String?): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setTimeZone(timezone: String, clientToken: String?): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun changeAdminCode(
        code: String,
        userName: String,
        timezone: String,
        clientToken: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun editToken(index: Int, permission: String, name: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun delete(clientToken: String?) {
//        if (useWifi && isNetworkConnectedUseCase())
            deleteByNetwork(clientToken)
//        else
//            deleteByBle()
    }
    private fun deleteByNetwork(clientToken: String?) {
        lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMap { lock ->
                lockInformationRepository.delete(lock)
                    .andThen(userCodeRepository.deleteAll(lockInfo.macAddress))
                    .andThen(eventLogRepository.deleteAll(lockInfo.macAddress))
                    .andThen(Single.just(lock.thingName ?: ""))
            }
            .toObservable()
            .asFlow()
            .flatMapConcat { flow { emit(iotService.delete(it, clientToken!!)) } }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(lockScope)
    }

    fun collectWifiList(): Flow<WifiListResult> = statefulConnection.collectWifiList(lockInfo)

    suspend fun scanWifi() = statefulConnection.scanWifi(lockInfo)

    fun collectConnectToWifiState(): Flow<WifiConnectState> = statefulConnection.collectConnectToWifiState(lockInfo)

    suspend fun connectLockToWifi(ssid: String, password: String): Boolean = statefulConnection.connectLockToWifi(ssid, password, lockInfo)

    fun getAndSaveThingName(clientToken: String) =
        flow { emit(iotService.getDeviceList(clientToken)) }
            .map {
                it.find { d -> d.attributes.bluetooth.mACAddress == lockInfo.macAddress }
                    ?.run {
                        lockInformationRepository.setThingName(lockInfo.macAddress, thingName)
                        Timber.d(thingName)
                        thingName
                    }
                    ?: throw Exception("device not found on remote")
            }
            .retry(5) {
                delay(5000)
                true
            }

    suspend fun deviceProvisionCreate(
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ): Boolean {
        return iotService.deviceProvisionCreate(
            serialNumber,
            deviceName,
            timeZone,
            timeZoneOffset,
            clientToken,
            model
        )
    }

    suspend fun lockByNetwork(clientToken: String?): LockSetting {

        val idToken = runCatching {
            getIdToken().single()
        }.getOrNull() ?: return LockSetting(LockConfig(LockOrientation.Left,false,false,false,0,null,null),LockStatus.LOCKED,0,0,0)

        return lockInformationRepository
            .get(lockInfo.macAddress)
            .toObservable()
            .asFlow()
            .flatMapConcat { flow { emit(iotService.lock(it.thingName!!, clientToken!!)) } }
            .map {
                LockSetting(
                    config = LockConfig(
                        orientation = LockOrientation.Left, //TODO
                        isSoundOn = false,
                        isVacationModeOn = false,
                        isAutoLock = false,
                        autoLockTime = 0,
                        latitude = null,
                        longitude = null
                    ),
                    status = LockStatus.LOCKED,
                    battery = 0,
                    batteryStatus = 0,
                    timestamp = 0
                )
            }
            .single()
    }

}