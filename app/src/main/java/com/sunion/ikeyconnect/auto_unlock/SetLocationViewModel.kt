package com.sunion.ikeyconnect.auto_unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.domain.usecase.device.LockSettingUseCase
import com.sunion.ikeyconnect.floorSix
import com.sunion.ikeyconnect.home.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SetLocationViewModel @Inject constructor(
    private val lockProvider: LockProvider,
    private val iotService: SunionIotService,
    private val getUuid: GetUuidUseCase,
    private val toastHttpException: ToastHttpException,
    private val statefulConnection: ReactiveStatefulConnection,
    private val lockSettingUseCase: LockSettingUseCase,
    private val autoUnlockUseCase: AutoUnlockUseCase,
): ViewModel(){
    private val _uiState = MutableStateFlow(SetLocationUiState())
    val uiState: StateFlow<SetLocationUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<SetLocationUiEvent>()
    val uiEvent: SharedFlow<SetLocationUiEvent> = _uiEvent

    private var _deviceIdentity: String? = null
    val deviceIdentity: String?
        get() = _deviceIdentity

    private var _deviceType: Int? = null
    val deviceType: Int?
        get() = _deviceType

    private var _location: LatLng? = null
    val location: LatLng?
        get() = _location

    private var lock: Lock? = null

    fun init(DeviceIdentity: String, deviceType: Int, latitude: Double, longitude: Double) {
        _deviceIdentity = DeviceIdentity
        statefulConnection.connectMacAddress = DeviceIdentity
        _deviceType = deviceType
        _location = LatLng(latitude, longitude)
        Timber.d("la= ${location?.latitude}, lo= ${location?.longitude}")
        _uiState.update { it.copy(deviceType = deviceType)}

        flow { emit(lockProvider.getLockByMacAddress(DeviceIdentity)) }
            .map {
                lock = it
            }
            .catch {
                Timber.e(it)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    fun setLocation(location: LatLng) {
        _location = location
    }

    fun setLocationToLock() {
        val lock = lock ?: return
        if(deviceType?.equals(HomeViewModel.DeviceType.WiFi.typeNum) == true){
            Timber.d("la= ${location?.latitude}, lo= ${location?.longitude}")
            flow { emit(lock.setLocation(deviceIdentity?:throw Exception("deviceIdentityNull"),(location?.latitude)?.floorSix()?:0.0, (location?.longitude)?.floorSix()?:0.0, getUuid.invoke())) }
                .map { lock.getLockConfig(deviceIdentity?:throw Exception("deviceIdentityNull"), getUuid.invoke()) }
                .flowOn(Dispatchers.IO)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .onCompletion {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onEach {
                    Timber.d("lock location:${it.latitude},${it.longitude}")
                    viewModelScope.launch { _uiEvent.emit(SetLocationUiEvent.SaveLocationSuccess) }
                }
                .catch {
                    Timber.e(it)
                    viewModelScope.launch { _uiEvent.emit(SetLocationUiEvent.SaveLocationFailed) }
                }
                .launchIn(viewModelScope)
        }else{
            flow { emit(lock.setLocationByBle((location?.latitude)?.floorSix()?:0.0, (location?.longitude)?.floorSix()?:0.0))}
                .flowOn(Dispatchers.IO)
                .onEach {
                    Timber.d("lock location:${it.latitude},${it.longitude}")
                    viewModelScope.launch { _uiEvent.emit(SetLocationUiEvent.SaveLocationSuccess) }
                }
                .catch {
                    Timber.e(it)
                    viewModelScope.launch { _uiEvent.emit(SetLocationUiEvent.SaveLocationFailed) }
                }
                .launchIn(viewModelScope)
        }

    }

}
data class SetLocationUiState(
    val isLoading: Boolean = false,
    val deviceType: Int = HomeViewModel.DeviceType.WiFi.typeNum,
)

sealed class SetLocationUiEvent {
    object SaveLocationSuccess : SetLocationUiEvent()
    object SaveLocationFailed : SetLocationUiEvent()
}