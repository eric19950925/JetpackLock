package com.sunion.ikeyconnect.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.google.gson.Gson
import com.sunion.ikeyconnect.ConnectMqttUiEvent
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.MqttStatefulConnection
import com.sunion.ikeyconnect.data.PreferenceStore
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetIdentityIdUseCase
import com.sunion.ikeyconnect.domain.usecase.device.CollectWIfiStateUseCase
import com.sunion.ikeyconnect.domain.usecase.device.GetAllLocksInfoUseCase
import com.sunion.ikeyconnect.domain.usecase.device.IsNetworkConnectedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.json.JSONTokener
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferenceStore: PreferenceStore,
    val mqttManager: AWSIotMqttManager,
    private val mqttStatefulConnection: MqttStatefulConnection,
    private val getIdToken: GetIdTokenUseCase,
    private val getAllLocksInfoUseCase: GetAllLocksInfoUseCase,
    private val getIdentityId: GetIdentityIdUseCase,
    private val gson: Gson,
    private val lockProvider: LockProvider,
    private val iotService: SunionIotService,
    private val isNetworkConnectedUseCase: IsNetworkConnectedUseCase,
    private val collectWIfiStateUseCase: CollectWIfiStateUseCase,
    ) :
    ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent

    private val lockDevices = mutableStateListOf<WiFiLock>()
    private var currentIndex = 0

    private val changeNameJobs: MutableMap<String, Job> = mutableMapOf()
    private val updateTimes: MutableMap<String, LocalDateTime> = mutableMapOf()
    private val hasBeenCollectedBleConnectStatus: MutableList<String> = mutableListOf()

    val connectionState: SharedFlow<ConnectMqttUiEvent> = mqttStatefulConnection.connectionState

    private val _showGuide = mutableStateOf(false)
    val showGuide: State<Boolean> = _showGuide

    private val _devicesFromMqtt = MutableSharedFlow<MutableList<WiFiLock>?>()
    private val devicesFromMqtt: SharedFlow<MutableList<WiFiLock>?> = _devicesFromMqtt


    init {
        _uiState.update { it.copy(showGuide = !preferenceStore.isGuidePopupMenuPressed) }

        collectMqttConnectionState()

        // network status
        _uiState.update { it.copy(networkAvailable = isNetworkConnectedUseCase()) }
        collectWIfiStateUseCase()
            .flowOn(Dispatchers.IO)
            .onEach { available -> _uiState.update { it.copy(networkAvailable = available) } }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

        getAllLocksInfoUseCase.invoke()
            .map {
                it.forEach {
                    Timber.d(it.macAddress + " " + it.keyTwo)
                }
            }.subscribeOn(Schedulers.io()).subscribe()
    }

    private fun collectMqttConnectionState() {
        connectionState
            .onEach {
                Timber.d(it.toString())
                when(it){
//                    loadLocks()
                    ConnectMqttUiEvent.Prepared -> mqttStatefulConnection.connectMqtt()
                    ConnectMqttUiEvent.Connected -> subDeviceList()
                    ConnectMqttUiEvent.ConnectionLost -> disconnectDeviceList()
                    ConnectMqttUiEvent.Reconnecting -> disconnectDeviceList()
                    else -> {}
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private suspend fun subDeviceList() {
        val idToken = runCatching {
            getIdToken().single()
        }.getOrNull() ?: return

        getIdentityId()
            .map { identityId ->
                Timber.d("identityId: $identityId")
                mqttStatefulConnection.subscribeApiPortal(idToken, identityId, callbackMqttDeviceList())
                if(mqttStatefulConnection.isReconnected){
                    mqttStatefulConnection.pubDeviceList(idToken, identityId)
                }
            }
            .launchIn(viewModelScope)
    }

    fun disconnectDeviceList() = runBlocking {
        _uiState.update { it.copy(networkAvailable = isNetworkConnectedUseCase()) }

        lockDevices.forEachIndexed { index, wiFiLock ->
            lockDevices[index] = lockDevices[index].copy( LockState = Reported(
                Battery = wiFiLock.LockState.Battery,
                Rssi = wiFiLock.LockState.Rssi,
                Status = wiFiLock.LockState.Status,
                RegistryVersion = wiFiLock.LockState.RegistryVersion,
                AccessCodeTime = wiFiLock.LockState.AccessCodeTime,
                Deadbolt = "disconnect network",
                Searchable = wiFiLock.LockState.Searchable,
                Direction = wiFiLock.LockState.Direction,
                Connected = false,))

            _uiState.update { if(it.locks.size > 0) {
                it.copy(locks = it.locks.toMutableList().apply {
                    set(index, lockDevices[index])
                })
            }
            else it.copy(locks = lockDevices)
            }
        }

    }

    private fun callbackMqttDeviceList() = AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
        try {
            collectDeviceFromMqtt()
            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            val shadow = Gson().fromJson(message, DeviceListResponse::class.java)
            Timber.d("callbackForMqttDeviceList: $shadow")
            runBlocking {
                //store the device into state lock list
                updateLocksByMqtt(shadow.ResponseBody.Devices?:return@runBlocking)
            }
        } catch (e: UnsupportedEncodingException) {
            Timber.d("Message encoding error.", e)
        }
    }

    private fun collectDeviceFromMqtt(){
        devicesFromMqtt
            .onEach { locks ->
                lockDevices.clear()
                lockDevices.addAll(locks ?: return@onEach)
//                _uiState.update { it.copy(locks = locks) }
            }
            .take(1)//test
            .flatMapConcat {
                flowForGetThingShadow(it?: throw IllegalArgumentException("Device list is empty.") )
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun flowForGetThingShadow(list: List<WiFiLock>) = flow { emit (
        list.forEach {
            mqttStatefulConnection.subPubGetThingShadow(it.ThingName, callbackMqttGetThingShadow())
            if(mqttStatefulConnection.isReconnected){
                mqttStatefulConnection.pubGetThingShadow(it.ThingName)
            }
            subPubUpdateDeviceShadow(it.ThingName)
        }
    )}

    fun setCurrentPage(page: Int) {
        Timber.d("page:$page")
        _uiState.update { it.copy(currentPage = page) }
        currentIndex = page

        loadLockState(page)
    }

    private fun callbackMqttGetThingShadow() = AWSIotMqttNewMessageCallback { _: String?, data: ByteArray? ->
        try {

            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            Timber.d("callbackMqttGetThing Msg: $message")
            val jsonObject = JSONTokener(message).nextValue() as JSONObject
            val lockState = jsonObject.getJSONObject("state")
            val clientToken = jsonObject.getString("clientToken")

            val reported = lockState.getString("reported")
            val shadowReported = Gson().fromJson(reported, Reported::class.java)
            Timber.d("callbackMqttGetThing: $shadowReported")
            runCatching {
                Timber.d("Lock Deadbolt: ${shadowReported.Deadbolt}")
                Timber.d("Lock Connected: ${shadowReported.Connected}")
                updateOneLockByMqtt(clientToken, shadowReported)
            }.onFailure { Timber.e(it) }
        } catch (e: UnsupportedEncodingException) {
            Timber.d("Message encoding error.", e)
        }
    }


    fun subPubUpdateDeviceShadow(thingName: String) =
        getIdToken()
            .map { idToken ->
                mqttStatefulConnection.subUpdateThingShadow(thingName, callbackMqttUpdateThingShadow(thingName))
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

    private fun callbackMqttUpdateThingShadow(thingName: String) = AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
        try {
            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            val jsonObject = JSONTokener(message).nextValue() as JSONObject
            val current = jsonObject.getJSONObject("current")

            val lockState = current.getJSONObject("state")
            val reported = lockState.getString("reported")
            val shadowReported = Gson().fromJson(reported, Reported::class.java)
            Timber.d(shadowReported.toString())
            runBlocking {
                Timber.d("Lock-${thingName.substring(0,3)} is updated by Mqtt: ${shadowReported.Deadbolt}")
                updateOneLockByMqtt(thingName, shadowReported)
            }
        } catch (e: UnsupportedEncodingException) {
            Timber.d("Message encoding error.", e)
        }
    }

    private fun updateOneLockByMqtt(thingName: String, lockState: Reported){
        lockDevices.indexOfFirst { it.ThingName == thingName }.let { index ->
            lockDevices[index] = lockDevices[index].copy( LockState = lockState )

            Timber.d("Update WiFiLock list: Lock-${thingName.substring(0,3)} "+lockDevices[index].toString())
            _uiState.update {
                if(it.locks.size > 0) {
                    it.copy(locks = it.locks.toMutableList().apply {
                        set(index, lockDevices[index])
                    })
                }
                else it.copy(locks = lockDevices)
            }
        }
    }

    private suspend fun updateLocksByMqtt(devices: List<DeviceThing>) {
        val wifiLockList = mutableListOf<WiFiLock>()
        devices.forEach { deviceThing ->
            val wifilock = WiFiLock(
                ThingName = deviceThing.ThingName,
                Attributes = deviceThing.Attributes,
                LockState = Reported(0,0,0,0,0,"","unknown",0,false)
            )
            wifiLockList.add(wifilock)
        }
        Timber.d("WiFiLock list is prepared by device list: $wifiLockList")
        _devicesFromMqtt.emit(wifiLockList)
    }

    fun isConnected(macAddress: String): Boolean =
        uiState.value.locks.find { it.Attributes.Bluetooth.MACAddress == macAddress }?.LockState?.Connected ?: false

    fun onLockClick() {
        val lock = lockDevices[currentIndex]

        //if is loading return

        if(!lock.LockState.Connected)return

        getIdToken()
            .map { idToken ->
                if(lock.LockState.Direction == "unknown"){
                    iotService.checkOrientation(lock.ThingName, idToken)
                }else {
                    if(lock.LockState.Deadbolt == "unlock")iotService.lock(lock.ThingName, lock.ThingName)
                    else iotService.unlock(lock.ThingName, lock.ThingName)
                }
            }.flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    fun getUpdateTime(thingName: String) = 0
    fun setLockName(thingName: String, name: String){}
    fun saveName(thingName: String){}

    private fun loadLockState(index: Int) {
        if (lockDevices.isEmpty() || index > lockDevices.lastIndex)
            return

        val currentLock = lockDevices[index]

//        collectedBleConnectStatus(bleDevice)
        //Todo : Must use model homeLock 紀錄所有資訊與狀態

//        if (true/*currentLock is ConnectedDevice*/) {
//            flow { emit(lockProvider.getLockByMacAddress(currentLock.macAddress)) }
//                .flatMapConcat {
//
//                }
//                .flowOn(Dispatchers.IO)
////                .onStart { setLockIsProcessing(index, true) }
////                .onCompletion {
////                    setLockIsProcessing(index, false)
////                    _uiState.update { it.copy(locks = lockDevices) }
////                }
//                .onEach {
//
//                }
//                .catch { Timber.e(it) }
//                .launchIn(viewModelScope)
//        }
    }

//    private fun setLockIsProcessing(index: Int, isProcessing: Boolean) {
//        _uiState.update {
//            it.copy(locks = uiState.value.locks.toMutableList().apply {
//                set(
//                    index,
//                    when (val lock = lockDevices[index]) {
//                        is ConnectedDevice -> lock.copy(isProcessing = isProcessing)
//                        is DisconnectedDevice -> lock.copy(isProcessing = isProcessing)
//                        is BoltOrientationFailDevice -> lock.copy(isProcessing = isProcessing)
//                        else -> lock
//                    }
//                )
//            })
//        }
//    }
    @OptIn(FlowPreview::class)
    fun boltOrientation(macAddress: String) {
//        loadLocksFlow()
//            .flatMapConcat { flow { emit(lockProvider.getLockByMacAddress(macAddress)) } }
//            .flatMapConcat { flow { emit(it!!.getBoltOrientation(getClientTokenUseCase())) } }
//            .flowOn(Dispatchers.IO)
//            .onStart {
//                Timber.d(lockDevices.toString())
//                lockDevices
//                    .indexOfFirst { it.macAddress == macAddress }
//                    .takeIf { it >= 0 }
//                    ?.let { targetIndex ->
//                        val lock = lockDevices[targetIndex]
//                        if (targetIndex >= 0 && lock is ConnectedDevice)
//                            setLockIsProcessing(targetIndex, true)
//                    }
//            }
//            .onEach { boltOrientation ->
//                Timber.d("boltOrientation:$boltOrientation")
//                viewModelScope.launch {
//                    _uiEvent.emit(
//                        if (boltOrientation != LockOrientation.NotDetermined) HomeUiEvent.BoltOrientationSuccess
//                        else HomeUiEvent.BoltOrientationFailed
//                    )
//                }
//                lockDevices
//                    .indexOfFirst { it.macAddress == macAddress }
//                    .takeIf { it >= 0 }
//                    ?.let { targetIndex ->
//                        val lock = lockDevices[targetIndex]
//                        if (targetIndex >= 0 && boltOrientation == LockOrientation.NotDetermined)
//                            _uiState.update {
//                                it.copy(
//                                    locks = lockDevices.toMutableList().apply {
//                                        set(
//                                            targetIndex,
//                                            BoltOrientationFailDevice(
//                                                macAddress = lock.macAddress,
//                                                connectionState = lock.connectionState,
//                                                permission = lock.permission,
//                                                name = lock.name,
//                                                createdAt = lock.createdAt,
//                                                lockOrientation = lock.lockOrientation,
//                                                isLocked = LockStatus.UNLOCKED,//TODO
//                                                displayIndex = lock.displayIndex
//                                            )
//                                        )
//                                    }
//                                )
//                            }
//                    }
//            }
//            .catch { Timber.e(it) }
//            .launchIn(viewModelScope)
    }

    private fun getDeviceListByNetwork() {
        getIdToken()
            .map { idToken ->
                iotService.getDeviceList("clientToken")
            }
            .map {
                it.forEach {
                    Timber.d(it.toString())
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    fun setGuideHasBeenSeen() {
        preferenceStore.isGuidePopupMenuPressed = true
        _uiState.update { it.copy(showGuide = false) }
    }


}

data class HomeUiState(
    val showGuide: Boolean = false,
    val locks: MutableList<WiFiLock> = mutableStateListOf(),
    val currentPage: Int = 0,
    val networkAvailable: Boolean = false,
)

sealed class HomeUiEvent {
    object BoltOrientationSuccess : HomeUiEvent()
    object BoltOrientationFailed : HomeUiEvent()
}