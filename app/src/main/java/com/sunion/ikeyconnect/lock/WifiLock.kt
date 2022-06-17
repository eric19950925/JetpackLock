package com.sunion.ikeyconnect.lock

import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.ikeyconnect.domain.Interface.*
import com.sunion.ikeyconnect.domain.blelock.BluetoothConnectState
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.blelock.WifiListCommand
import com.sunion.ikeyconnect.domain.exception.EmptyLockInfoException
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.model.LockInfo
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    ) : Lock, SunionWifiService {

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
    val connectionState2: SharedFlow<BluetoothConnectState> = _connectionState2

//    private val _connectionState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connectionState: SharedFlow<Event<Pair<Boolean, String>>> = statefulConnection.connectionState

    fun init(lockInfo: LockInfo): WifiLock {
        this._lockInfo = lockInfo
        return this
    }

    override fun connect() {
        statefulConnection.connect_asflow(lockInfo.macAddress)
//        statefulConnection.establishConnection(lockInfo.macAddress, false)
        Timber.d("find ${lockInfo.deviceName}")
//        statefulConnection.establishConnection_by_deviceName(lockInfo.deviceName, false)
        }

    override fun disconnect() {
        connectionDisposable?.dispose()
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
        TODO("Not yet implemented")
    }

    override fun collectWifiList(): Flow<WifiListResult> = statefulConnection.collectWifiList()

    override suspend fun scanWifi() = statefulConnection.scanWifi()

    override fun collectConnectToWifiState(): Flow<WifiConnectState> = statefulConnection.collectConnectToWifiState()

    override suspend fun connectLockToWifi(ssid: String, password: String): Boolean = statefulConnection.connectLockToWifi(ssid, password)

//    suspend fun deviceProvisionCreate(
//        serialNumber: String,
//        deviceName: String,
//        timeZone: String,
//        timeZoneOffset: Int,
//        clientToken: String,
//        model: String,
//    ): Boolean {
//        return iotService.deviceProvisionCreate(
//            serialNumber,
//            deviceName,
//            timeZone,
//            timeZoneOffset,
//            clientToken,
//            model
//        )
//    }
}

data class WifiConnectState(
    val isWifiConnected: Boolean = false,
    val isIotConnected: Boolean = false,
)