package com.sunion.ikeyconnect.add_lock.pairing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import javax.inject.Inject
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.exception.ConnectionTokenException
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.EventState
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.device.IsBlueToothEnabledUseCase
import com.sunion.ikeyconnect.lock.WifiLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val isBlueToothEnabledUseCase: IsBlueToothEnabledUseCase,
    private val getClientTokenUseCase: GetClientTokenUseCase,
    private val lockProvider: LockProvider
) :
    ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<PairingUiEvent>()
    val uiEvent: SharedFlow<PairingUiEvent> = _uiEvent

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress
    private var lock: Lock? = null

    @OptIn(FlowPreview::class)
    fun init(macAddress: String) {
        this._macAddress = macAddress
        flow { emit(lockProvider.getLockByMacAddress(macAddress)) }
            .onEach {
                lock = it
//                _uiState.update { state -> state.copy(lockIsWifi = lock is WifiLock) }
            }
//            .flatMapConcat { it!!.connectionState }
//            .flowOn(Dispatchers.IO)
//            .onEach { event ->
//                val lock = lock ?: return@onEach
//                Timber.d("event status:${event.status} data:${event.data} message:${event.message}")
//                val connectionState = PairingUiState.from(event)
//                _uiState.update { state -> state.copy(connectionState = connectionState) }
//
//                when (event.message) {
//                    ConnectionTokenException.DeviceRefusedException::class.java.simpleName,
//                    ConnectionTokenException.IllegalTokenException::class.java.simpleName -> {
//                        _uiEvent.emit(PairingUiEvent.UserNoPermissionAccessLock)
//                        deleteLock()
//                    }
//                }
//
//                if (connectionState == PairingUiState.ConnectionState.Done) {
//                    setLockTime(lock)
//                    cacheDefaultLockName(lock)
//                    event.data?.let { (isConnected, _) ->
//                        if (isConnected)
//                            _uiState.update { it.copy(shouldShowNext = lock.lockInfo?.isOwnerToken?: return@let) }
//                    }
//                }
//            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

//        collectBluetoothAvailableState()
    }

    fun checkIsBluetoothEnable(): Boolean {
        val isBlueToothEnabled = isBlueToothEnabledUseCase()
        _uiState.update {
            it.copy(
                shouldShowBluetoothEnableDialog = !isBlueToothEnabled,
                isBlueToothAvailable = isBlueToothEnabled
            )
        }
        return isBlueToothEnabled
    }
    fun closeBluetoothEnableDialog() {
        _uiState.update { it.copy(shouldShowBluetoothEnableDialog = false) }
    }
    fun startPairing() {
        if (!checkIsBluetoothEnable())
            return

        lock?.connect()
    }
    fun closeExitPromptDialog() {
        _uiState.update { it.copy(shouldShowExitDialog = false) }
    }

    fun showExitPromptDialog() {
        _uiState.update { it.copy(shouldShowExitDialog = true) }
    }

    fun deleteLock() {
        flow { emit(lock!!.delete(getClientTokenUseCase())) }
            .onEach {
                _uiEvent.emit(PairingUiEvent.DeleteLockSuccess)
                lock?.let {
                    if (it.isConnected())
                        it.disconnect()
                }
            }
            .catch {
                Timber.e(it)
                _uiEvent.emit(PairingUiEvent.DeleteLockFail)
            }
            .launchIn(viewModelScope)
    }

}

data class PairingUiState(
    val isBlueToothAvailable: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.None,
    val shouldShowBluetoothEnableDialog: Boolean = false,
    val shouldShowExitDialog: Boolean = false,
    val shouldShowNext: Boolean = false,
    val lockIsWifi: Boolean = false
) {
    enum class ConnectionState { None, Processing, Error, Done }

    companion object {
        fun from(event: Event<Pair<Boolean, String>>): ConnectionState = when {
            event.status == EventState.ERROR -> ConnectionState.Error
            event.status == EventState.LOADING -> ConnectionState.Processing
            event.status == EventState.SUCCESS && event.data?.first == true -> ConnectionState.Done
            else -> ConnectionState.None
        }
    }
}

sealed class PairingUiEvent {
    object UserNoPermissionAccessLock : PairingUiEvent()
    object DeleteLockSuccess : PairingUiEvent()
    object DeleteLockFail : PairingUiEvent()
}