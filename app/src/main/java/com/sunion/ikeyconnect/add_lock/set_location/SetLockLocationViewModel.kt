package com.sunion.ikeyconnect.add_lock.set_location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SetLockLocationViewModel @Inject constructor(
    private val lockProvider: LockProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SetLockLocationUiState())
    val uiState: StateFlow<SetLockLocationUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<SetLockLocationUiEvent>()
    val uiEvent: SharedFlow<SetLockLocationUiEvent> = _uiEvent

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress

    private var lock: Lock? = null

    private var location = LatLng(25.0330, 121.5654)

    fun init(macAddress: String) {
        _macAddress = macAddress
        viewModelScope.launch(Dispatchers.IO) {
            lock = lockProvider.getLockByMacAddress(macAddress)
        }
    }

    fun setLocation(location: LatLng) {
        this.location = location
    }

    fun setLocationToLock() {
        val lock = lock ?: return
        flow { emit(lock.setLocation(location.latitude, location.longitude)) }
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

data class SetLockLocationUiState(
    val initLocation: LatLng = LatLng(25.0330, 121.5654)
)

sealed class SetLockLocationUiEvent {
    object SaveSuccess : SetLockLocationUiEvent()
    object SaveFailed : SetLockLocationUiEvent()
}