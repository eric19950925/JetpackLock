package com.sunion.ikeyconnect.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.google.gson.Gson
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    val connectionState: SharedFlow<Boolean> = mqttStatefulConnection.connectionState

    private val _showGuide = mutableStateOf(false)
    val showGuide: State<Boolean> = _showGuide

    private val _devicesFromMqtt = MutableSharedFlow<MutableList<WiFiLock>?>()
    val devicesFromMqtt: SharedFlow<MutableList<WiFiLock>?> = _devicesFromMqtt


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
    }

    private fun collectMqttConnectionState() {
        connectionState
            .onEach {
                if (it){
//                    loadLocks()
                    subDeviceList()
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
            }
            .launchIn(viewModelScope)
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
                _uiState.update { it.copy(locks = locks) }
            }
            .take(1)//test
            .flatMapConcat {
                flowForGetDeviceShadow(it?: throw IllegalArgumentException("Device list is empty.") )
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun flowForGetDeviceShadow(list: List<WiFiLock>) = flow { emit (
        list.forEach {
            mqttStatefulConnection.subPubGetDeviceShadow(it.ThingName, callbackMqttGetThingShadow())
            subPubUpdateDeviceShadow(it.ThingName)
        }
    )}

    fun setCurrentPage(page: Int) {
        Timber.d("page:$page")
        _uiState.update { it.copy(currentPage = page) }
        currentIndex = page

        loadLockState(page)
    }

    private fun callbackMqttGetThingShadow() = AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
        try {

            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            val shadow = Gson().fromJson(message, LockInfomation::class.java)
            Timber.d("callbackMqttGetThingShadow: $shadow")
            runBlocking {
                Timber.d("Lock Deadbolt: ${shadow.state.reported.Deadbolt == "lock"}")
                Timber.d("Lock Connected: ${shadow.state.reported.Connected}")
//                _lockUiState.emit(shadow.state.reported.Deadbolt == "lock")
                updateOneLockByMqtt(shadow.clientToken, shadow.state.reported)
            }
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

            val shadow = Gson().fromJson(message, DeviceShadowDoc::class.java)
            Timber.d(shadow.toString())
            runBlocking {
                Timber.d("Lock-${thingName.substring(0,3)} is updated by Mqtt: ${shadow.current.state.reported.Deadbolt}")
//                _lockUiState.emit(shadow.current.state.reported.Deadbolt == "lock")

                updateOneLockByMqtt(thingName, shadow.current.state.reported)
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
                it.copy(locks = it.locks.toMutableList().apply {
                    set(index, lockDevices[index])
                })
            }
        }
    }

    private suspend fun updateLocksByMqtt(devices: List<DeviceThing>) {
        val wifiLockList = mutableListOf<WiFiLock>()
        devices.forEach { deviceThing ->
            val wifilock = WiFiLock(
                ThingName = deviceThing.ThingName,
                Attributes = deviceThing.Attributes,
                LockState = Reported(0,0,0,0,0,"","",0,false)
            )
            wifiLockList.add(wifilock)
        }
        Timber.d("WiFiLock list is prepared by device list: $wifiLockList")
        _devicesFromMqtt.emit(wifiLockList)
    }

    fun isConnected(thingName: String): Boolean =
        uiState.value.locks.find { it.ThingName == thingName }?.LockState?.Connected ?: false

    fun onLockClick() {
        val lock = lockDevices[currentIndex]
        getIdToken()
//            .take(1)
            .map { idToken ->
                if(lock.LockState.Deadbolt == "unlock")iotService.lock(idToken, lock.ThingName, lock.ThingName)
                else iotService.unlock(idToken, lock.ThingName, lock.ThingName)
            }.flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }


    /*
    private fun loadLocksFlow() =
        getAllLocksInfoUseCase.invoke()
            .toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .onEach { locks ->
                lockDevices.clear()
                lockDevices.addAll(locks)
                _uiState.update { it.copy(locks = locks) }
            }
            .catch { Timber.e(it) }

     */
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
                iotService.getDeviceList(idToken, "clientToken")
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