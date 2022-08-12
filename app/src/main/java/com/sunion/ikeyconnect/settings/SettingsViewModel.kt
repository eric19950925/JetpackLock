package com.sunion.ikeyconnect.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.RegistryGetResponse
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.home.HomeViewModel
import com.sunion.ikeyconnect.lock.AllLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getClientTokenUseCase: GetClientTokenUseCase,
    private val lockProvider: LockProvider,
    private val iotService: SunionIotService,
    private val getUuid: GetUuidUseCase,
    private val toastHttpException: ToastHttpException,
) :
    ViewModel() {

    override fun onCleared() {
        super.onCleared()
        Timber.tag("SettingsViewModel").d("onCleared")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _newRegistryAttributes = MutableStateFlow<RegistryGetResponse.RegistryPayload.RegistryAttributes?>(null)
    val newRegistryAttributes: StateFlow<RegistryGetResponse.RegistryPayload.RegistryAttributes?> = _newRegistryAttributes

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent: SharedFlow<SettingsUiEvent> = _uiEvent

    private var _deviceIdentity: String? = null
    val deviceIdentity: String?
        get() = _deviceIdentity

    private val _adminCode = mutableStateOf("")
    val adminCode: State<String> = _adminCode

    private var _isConnected: Boolean = false
    val isConnected: Boolean
        get() = _isConnected

    private var _battery: String = "0"
    val battery: String
        get() = _battery

    fun init(DeviceIdentity: String, isConnected: Boolean, battery: String, deviceType: Int) {
        _deviceIdentity = DeviceIdentity
        _isConnected = isConnected
        _battery = battery
        _uiState.update { it.copy(macAddressOrThingName = DeviceIdentity, battery = battery,)}
        Timber.d("$deviceIdentity , $isConnected")
        if(deviceType == HomeViewModel.DeviceType.WiFi.typeNum && isConnected)getRegistry() else {
            //cmd d6
        }
    }

    fun leaveSettingPage(onNext: () -> Unit) {
        //for wifi lock , need to check and update
        //if type = wifi
        if(
            uiState.value.registryAttributes.autoLock == newRegistryAttributes.value?.autoLock &&
            uiState.value.registryAttributes.autoLockDelay == newRegistryAttributes.value?.autoLockDelay &&
            uiState.value.registryAttributes.vacationMode == newRegistryAttributes.value?.vacationMode &&
            uiState.value.registryAttributes.keyPressBeep == newRegistryAttributes.value?.keyPressBeep &&
            uiState.value.registryAttributes.preamble == newRegistryAttributes.value?.preamble &&
            uiState.value.registryAttributes.secureMode == newRegistryAttributes.value?.secureMode
        ){
            Timber.d(newRegistryAttributes.value.toString())
            onNext()
        }
        else if (newRegistryAttributes.value == null){
            //disconnect wifi
            onNext()
        }
        else {
            Timber.d(newRegistryAttributes.value.toString())
            flow { emit(iotService.updateDeviceRegistry(
                deviceIdentity?:throw Exception("deviceIdentity is null"),
                newRegistryAttributes.value?:throw Exception("newRegistryAttributes is null"),
                getUuid.invoke()
            )) }
                .flowOn(Dispatchers.IO)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .onCompletion { _uiState.update { it.copy(isLoading = false) } ; onNext()}
                .catch { e ->
                    toastHttpException(e)
                }.launchIn(viewModelScope)
        }
    }

    fun delete() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun onFactoryResetClick() {
        _uiState.update { it.copy(showFactoryResetDialog = true) }
    }

    fun onFactoryResetDialogComfirm(){
        Timber.d("adminCode = ${adminCode.value}")
        flow { emit(iotService.getAdminCode(
            deviceIdentity?:throw Exception("deviceIdentity is null"),
            getUuid.invoke()
        ))}
            .map { accessCodeGetResponse ->
                if(accessCodeGetResponse.accessCode.isEmpty())throw Exception("You had not set admin code.")
                (accessCodeGetResponse.accessCode[0]?.code ?: "") == adminCode.value
            }
            .map {
                if(it)deleteLock()
                else throw Exception("Admin Code Error.")
            }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .catch { e ->
                toastHttpException(e)
                _uiState.update { it.copy(isLoading = false) }
            }.launchIn(viewModelScope)
    }

    private fun deleteLock(){
        flow {
            emit(iotService.delete(deviceIdentity?:throw Exception("deviceIdentity is null"), getUuid.invoke()))
        }
            .flowOn(Dispatchers.IO)
            .onCompletion {
                _uiState.update { it.copy(isLoading = false) }
                viewModelScope.launch { _uiEvent.emit(SettingsUiEvent.DeleteLockSuccess) }
            }
            .catch { e ->
                toastHttpException(e)
                viewModelScope.launch { _uiEvent.emit(SettingsUiEvent.DeleteLockFail) }
            }.launchIn(viewModelScope)
    }

    fun setAdminCode(code: String) {
        _adminCode.value = code
    }


    fun closeFactoryResetDialog() {
        _uiState.update { it.copy(showFactoryResetDialog = false) }
    }

    @OptIn(FlowPreview::class)
    fun executeDelete() {
        val macAddress = deviceIdentity ?: return
        var lock: Lock? = null
        flow { emit(lockProvider.getLockByMacAddress(macAddress)!!) }
            .flatMapConcat { inLock ->
                lock = inLock
                flow {
                    emit(
                        if (lock is AllLock)
                            inLock
                        else
                            if (!inLock.isConnected()) {
                                inLock.connect()
                                inLock
                            } else inLock
                    )
                }
            }
            .flatMapConcat {
                when {
                    it.isConnected() || it is AllLock -> flow {
                        emit(Event(status = EventState.SUCCESS, data = Pair(true, "")))
                    }
                    else -> it.connectionState
                }
            }
            .filter {
                Timber.d("${it.status} ${it.data}")
                if (it.status == EventState.ERROR) {
                    throw Exception(it.message)
                }
                it.status == EventState.SUCCESS && it.data?.first == true
            }
            .flatMapConcat { flow { emit(lock!!.delete(getClientTokenUseCase())) } }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .onEach {
//                lockProvider.deleteLockCache(macAddress) Todo
                viewModelScope.launch { _uiEvent.emit(SettingsUiEvent.DeleteLockSuccess) }
            }
            .catch {
                Timber.e(it)
                viewModelScope.launch { _uiEvent.emit(SettingsUiEvent.DeleteLockFail) }
            }
            .launchIn(viewModelScope)
    }

    fun closeDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    private fun getRegistry(){
        flow { emit(iotService.getDeviceRegistry(deviceIdentity?:throw Exception("deviceIdentity is null"), getUuid.invoke()))}
            .map {
                var info : RegistryGetResponse.RegistryPayload.RegistryAttributes? = null
                it.payload.attributes.let{ attributes ->
                    info = RegistryGetResponse.RegistryPayload.RegistryAttributes(
                        autoLock = attributes.autoLock,
                        autoLockDelay = attributes.autoLockDelay,
                        deviceName = attributes.deviceName,
                        preamble = attributes.preamble,
                        secureMode = attributes.secureMode,
                        syncing = attributes.syncing,
                        model = attributes.model,
                        firmwareVersion = attributes.firmwareVersion,
                        keyPressBeep = attributes.keyPressBeep,
                        offlineNotifiy = attributes.offlineNotifiy,
                        statusNotification = attributes.statusNotification,
                        timezone = RegistryGetResponse.RegistryPayload.RegistryAttributes.Timezone(
                            offset = attributes.timezone.offset,
                            shortName = attributes.timezone.shortName
                        ),
                        vacationMode = attributes.vacationMode,
                        wifi = RegistryGetResponse.RegistryPayload.RegistryAttributes.WiFiInfo(
                            passphrase = attributes.wifi.passphrase,
                            SSID = attributes.wifi.SSID,
                            security = attributes.wifi.security
                        ),
                        bluetooth = RegistryGetResponse.RegistryPayload.RegistryAttributes.Bluetooth(
                            broadcastName = attributes.bluetooth?.broadcastName?:"",
                            macAddress = attributes.bluetooth?.macAddress?:"",
                            connectionKey = attributes.bluetooth?.connectionKey?:"",
                            shareToken = attributes.bluetooth?.shareToken?:""
                        ),
                        location = RegistryGetResponse.RegistryPayload.RegistryAttributes.LocationInfo(
                            latitude = attributes.location.latitude,
                            longitude = attributes.location.longitude
                        )
                    )
                }
                _uiState.update { state -> state.copy(registryAttributes = info ?: throw Exception("RegistryAttributes is mull")) }
                _newRegistryAttributes.value = info
            }.flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                toastHttpException(e)
            }.launchIn(viewModelScope)
    }

    fun setVacationMode(isEnable: Boolean) {
        _newRegistryAttributes.update { it?.copy(vacationMode = isEnable) }
        /*
        flow { emit(iotService.updateVacationMode(
            deviceIdentity?:throw Exception("deviceIdentity is null"),
            isEnable,
            getUuid.invoke()
        )) }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                Timber.e(e)
            }.launchIn(viewModelScope)

         */
    }

    fun setAutoLock(isEnable: Boolean) {
        _newRegistryAttributes.update { it?.copy(autoLock = isEnable) }
        /*
        flow { emit(iotService.updateAutoLock(
            deviceIdentity?:throw Exception("deviceIdentity is null"),
            isEnable,
            uiState.value.registryAttributes.autoLockDelay,
            getUuid.invoke()
        )) }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                toastHttpException(e)
            }.launchIn(viewModelScope)

         */
    }

    fun editClick(isClicked: Boolean) {
        _uiState.update { it.copy(isAutoLockEditClicked = !isClicked) }
    }

    fun onAutoLockDone(delay: Int?){
        Timber.d("delay = $delay")
        _newRegistryAttributes.update { it?.copy(autoLockDelay = delay ?: 2) }
        /*
        flow { emit(iotService.updateAutoLock(
            deviceIdentity?:throw Exception("deviceIdentity is null"),
            true,
            delay?:2,
            getUuid.invoke()
        )) }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                toastHttpException(e)
            }.launchIn(viewModelScope)

         */
    }

    fun setKeyPressBeep(isEnable: Boolean) {
        _newRegistryAttributes.update { it?.copy(keyPressBeep = isEnable) }
        /*
        flow { emit(iotService.updateKeyPressBeep(
            deviceIdentity?:throw Exception("deviceIdentity is null"),
            isEnable,
            getUuid.invoke()
        )) }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                Timber.e(e)
            }.launchIn(viewModelScope)

         */
    }

    fun setScureMode(isEnable: Boolean) {
        _newRegistryAttributes.update { it?.copy(secureMode = isEnable) }
        /*
        flow { emit(iotService.updateSecureMode(
            deviceIdentity?:throw Exception("deviceIdentity is null"),
            isEnable,
            getUuid.invoke()
        )) }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                Timber.e(e)
            }.launchIn(viewModelScope)

         */
    }

    fun setPreamble(isEnable: Boolean) {
        _newRegistryAttributes.update { it?.copy(preamble = isEnable) }
        /*
        flow { emit(iotService.updatePreamble(
            deviceIdentity?:throw Exception("deviceIdentity is null"),
            isEnable,
            getUuid.invoke()
        )) }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                Timber.e(e)
            }.launchIn(viewModelScope)

         */
    }

}

data class SettingsUiState(
    val showDeleteConfirmDialog: Boolean = false,
    val showFactoryResetDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isAutoLockEditClicked: Boolean = false,
    val macAddressOrThingName: String = "",
    val battery: String = "0",
    val registryAttributes: RegistryGetResponse.RegistryPayload.RegistryAttributes =
        RegistryGetResponse.RegistryPayload.RegistryAttributes(
            autoLock = false,
            autoLockDelay = 0,
            deviceName = "",
            preamble = false,
            secureMode = false,
            syncing = false,
            model = "",
            firmwareVersion = "",
            keyPressBeep = false,
            offlineNotifiy = false,
            statusNotification = false,
            timezone = RegistryGetResponse.RegistryPayload.RegistryAttributes.Timezone(
                offset = 0,
                shortName = ""
            ),
            vacationMode = false,
            wifi = RegistryGetResponse.RegistryPayload.RegistryAttributes.WiFiInfo(
                passphrase = "",
                SSID = "",
                security = ""
            ),
            bluetooth = RegistryGetResponse.RegistryPayload.RegistryAttributes.Bluetooth(
                broadcastName = "",
                macAddress = "",
                connectionKey = "",
                shareToken = ""
            ),
            location = RegistryGetResponse.RegistryPayload.RegistryAttributes.LocationInfo(
                latitude = 0.0,
                longitude = 0.0
            )
        )
)

sealed class SettingsUiEvent {
    object DeleteLockSuccess : SettingsUiEvent()
    object DeleteLockFail : SettingsUiEvent()
}