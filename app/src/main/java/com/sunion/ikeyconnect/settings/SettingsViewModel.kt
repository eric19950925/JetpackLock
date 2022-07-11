package com.sunion.ikeyconnect.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.lock.WifiLock
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
) :
    ViewModel() {

    override fun onCleared() {
        super.onCleared()
        Timber.tag("SettingsViewModel").d("onCleared")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent: SharedFlow<SettingsUiEvent> = _uiEvent

    private var _macAddressOrThingName: String? = null
    val macAddressOrThingName: String?
        get() = _macAddressOrThingName

    private var _isConnected: Boolean = false
    val isConnected: Boolean
        get() = _isConnected

    fun init(macAddressOrThingName: String, isConnected: Boolean) {
        _macAddressOrThingName = macAddressOrThingName
        _isConnected = isConnected
        _uiState.update { it.copy(macAddressOrThingName = macAddressOrThingName)}
        Timber.d("$macAddressOrThingName , $isConnected")

        flow { emit(iotService.getDeviceList(getUuid.invoke())) }
            .flowOn(Dispatchers.IO)
            .map {
                it.find { it.thingName == macAddressOrThingName }?:throw Exception("Not found thing")
            }
            .map {
                val wifilock = WiFiLock(
                    ThingName = it.thingName,
                    Attributes = DeviceAttributes(
                        DeviceBleInfo(
                            it.attributes.bluetooth.broadcastName,
                            it.attributes.bluetooth.connectionKey,
                            it.attributes.bluetooth.mACAddress,
                            it.attributes.bluetooth.shareToken,
                        ),
                        it.attributes.deviceName,
                        it.attributes.syncing,
                        it.attributes.vacationMode
                    ),
                    LockState = Reported(0,0,0,0,0,"","unknown",0,false)
                )
                wifilock
                _uiState.update { state -> state.copy(wifiLock = wifilock) }
            }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .catch { e ->
                Timber.e(e)
            }.launchIn(viewModelScope)


    }

    fun delete() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    @OptIn(FlowPreview::class)
    fun executeDelete() {
        val macAddress = macAddressOrThingName ?: return
        var lock: Lock? = null
        flow { emit(lockProvider.getLockByMacAddress(macAddress)!!) }
            .flatMapConcat { inLock ->
                lock = inLock
                flow {
                    emit(
                        if (lock is WifiLock)
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
                    it.isConnected() || it is WifiLock -> flow {
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
}

data class SettingsUiState(
    val showDeleteConfirmDialog: Boolean = false,
    val isLoading: Boolean = false,
    val macAddressOrThingName: String = "",
    val wifiLock: WiFiLock = WiFiLock(
        ThingName = "",
        Attributes = DeviceAttributes(
            DeviceBleInfo(
                "",
                "",
                "",
                "",
            ),
            "",
            false,
            false
        ),
        LockState = Reported(0,0,0,0,0,"","unknown",0,false),)
)

sealed class SettingsUiEvent {
    object DeleteLockSuccess : SettingsUiEvent()
    object DeleteLockFail : SettingsUiEvent()
}