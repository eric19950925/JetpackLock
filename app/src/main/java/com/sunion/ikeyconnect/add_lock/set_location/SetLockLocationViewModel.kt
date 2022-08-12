package com.sunion.ikeyconnect.add_lock.set_location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.floorSix
import com.sunion.ikeyconnect.home.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SetLockLocationViewModel @Inject constructor(
    private val lockProvider: LockProvider,
    private val provisionDomain: ProvisionDomain,
    private val getUuid: GetUuidUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SetLockLocationUiState())
    val uiState: StateFlow<SetLockLocationUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<SetLockLocationUiEvent>()
    val uiEvent: SharedFlow<SetLockLocationUiEvent> = _uiEvent

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress

    private var _deviceType: Int? = null
    val deviceType: Int?
        get() = _deviceType

    private var lock: Lock? = null

    private var location = LatLng(25.0330, 121.5654)

    fun init(macAddress: String, deviceType: Int) {
        _macAddress = macAddress
        _deviceType = deviceType
        viewModelScope.launch(Dispatchers.IO) {
            lock = lockProvider.getLockByMacAddress(macAddress)
        }
    }

    fun setLocation(location: LatLng) {
        this.location = location
    }

    fun setLocationToLock() {
        val lock = lock ?: return
        if(deviceType?.equals(HomeViewModel.DeviceType.WiFi.typeNum) == true){
            Timber.d("la= ${location.latitude}, lo= ${location.longitude}")
            flow { emit(lock.setLocation(provisionDomain.provisionThingName, (location.latitude).floorSix(), (location.longitude).floorSix(), getUuid.invoke())) }
                .map { lock.getLockConfig(provisionDomain.provisionThingName, getUuid.invoke()) }
                .flowOn(Dispatchers.IO)
                .onStart { _uiState.update { it.copy(isProcessing = true) } }
                .onCompletion {
                    _uiState.update { it.copy(isProcessing = false) }
                }
                .onEach {
                    Timber.d("lock location:${it.latitude},${it.longitude}")
                    viewModelScope.launch { _uiEvent.emit(SetLockLocationUiEvent.SaveSuccess) }
                }
                .catch {
                    Timber.e(it)
                    viewModelScope.launch { _uiEvent.emit(SetLockLocationUiEvent.SaveFailed) }
                }
                .launchIn(viewModelScope)
        }else{
            flow { emit(lock.setLocationByBle((location.latitude).floorSix(), (location.longitude).floorSix()))}
                .flowOn(Dispatchers.IO)
                .onEach {
                    Timber.d("lock location:${it.latitude},${it.longitude}")
                    viewModelScope.launch { _uiEvent.emit(SetLockLocationUiEvent.SaveSuccess) }
                }
                .catch {
                    Timber.e(it)
                    viewModelScope.launch { _uiEvent.emit(SetLockLocationUiEvent.SaveFailed) }
                }
                .launchIn(viewModelScope)
        }

    }
}

data class SetLockLocationUiState(
    val initLocation: LatLng = LatLng(25.0330, 121.5654),
    val isProcessing: Boolean = false
)

sealed class SetLockLocationUiEvent {
    object SaveSuccess : SetLockLocationUiEvent()
    object SaveFailed : SetLockLocationUiEvent()
}