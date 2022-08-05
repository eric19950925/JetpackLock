package com.sunion.ikeyconnect.add_lock.admin_code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.blelock.BluetoothAvailableState
import com.sunion.ikeyconnect.domain.blelock.BluetoothAvailableStateCollector
import com.sunion.ikeyconnect.domain.model.DeviceToken
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.device.IsBlueToothEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AdminCodeViewModel @Inject constructor(
    private val isBlueToothEnabledUseCase: IsBlueToothEnabledUseCase,
    private val getClientTokenUseCase: GetClientTokenUseCase,
    private val bluetoothAvailableStateCollector: BluetoothAvailableStateCollector,
    private val lockProvider: LockProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminCodeUiState())
    val uiState: StateFlow<AdminCodeUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<AdminCodeUiEvent>()
    val uiEvent: SharedFlow<AdminCodeUiEvent> = _uiEvent

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress
    private var lock: Lock? = null

    private var isBlueToothAvailable = true
    private var job: Job? = null

    fun init(macAddress: String) {
        this._macAddress = macAddress
        viewModelScope.launch(Dispatchers.IO) {
            lock = lockProvider.getLockByMacAddress(macAddress)
            lock?.let {
                _uiState.update { state ->
                    state.copy(
                        hasAdminCodeBeenSet = it.hasAdminCodeBeenSet(),
                        shouldShowBluetoothEnableDialog = !isBlueToothEnabledUseCase()
                    )
                }
            }
        }
        collectBluetoothAvailableState()
    }

    fun setLockName(name: String) {
        _uiState.update { it.copy(lockName = name) }
        checkNextShouldEnableOrNot()
    }

    fun setAdminCode(code: String) {
        _uiState.update { it.copy(adminCode = code) }
        checkNextShouldEnableOrNot()
    }

    @OptIn(FlowPreview::class)
    fun execute() {
        val lock = lock ?: return
        val state = uiState.value
        job?.cancel()
        job = run {
            if (state.hasAdminCodeBeenSet)
                flow { emit(lock.editToken(0, DeviceToken.PERMISSION_ALL, state.userName)) }
            else
                flow {
                    emit(
                        lock.changeAdminCode(
                            state.adminCode,
                            state.userName,
                            state.timezone,
                            getClientTokenUseCase()
                        )
                    )
                }
        }
            .onStart { _uiState.update { it.copy(isProcessing = true) } }
            .onCompletion { _uiState.update { it.copy(isProcessing = false) } }
            .onEach {
                if (state.hasAdminCodeBeenSet)
                    Timber.d("token name has been set to ${state.userName}: $it")
                else
                    Timber.d("admin code has been set: $it")
            }
            .flatMapConcat { flow { emit(delay(300)) } }
            .flatMapConcat {
                flow { emit(lock.changeLockName(state.lockName, getClientTokenUseCase())) }
            }
            .onEach { Timber.d("lock name has been set to: ${state.lockName}: $it") }
            .flatMapConcat { flow { emit(delay(300)) } }
            .flatMapConcat {
                flow { emit(lock.setTimeZone(state.timezone, getClientTokenUseCase())) }
            }
            .onEach { isSuccessful ->
                Timber.d("change token name and change lock name isSuccessful: $isSuccessful")
                if (isSuccessful)
                    _uiEvent.emit(AdminCodeUiEvent.Success)
                else
                    _uiState.update { state1 -> state1.copy(errorMessage = "Failed to set admin code") }
            }
            .flowOn(Dispatchers.IO)
            .onEach { Timber.d("change token name and change lock name process complete") }
            .catch {
                Timber.e(it)
                _uiState.update { state1 -> state1.copy(errorMessage = "Failed to set admin code") }
            }
            .launchIn(viewModelScope)
    }

    fun showTimeZoneMenu() {
        _uiState.update { it.copy(showTimezoneMenu = true) }
    }

    fun closeTimeZoneMenu() {
        _uiState.update { it.copy(showTimezoneMenu = false) }
    }

    fun setTimezone(timezone: String) {
        _uiState.update { it.copy(timezone = timezone) }
        checkNextShouldEnableOrNot()
        closeTimeZoneMenu()
    }

    fun closeExitPromptDialog() {
        _uiState.update { it.copy(shouldShowExitDialog = false) }
    }

    fun showExitPromptDialog() {
        _uiState.update { it.copy(shouldShowExitDialog = true) }
    }


    fun closeBluetoothEnableDialog() {
        _uiState.update { it.copy(shouldShowBluetoothEnableDialog = false) }
    }

    fun checkIsBluetoothEnable() {
        isBlueToothAvailable = isBlueToothEnabledUseCase()
        _uiState.update { it.copy(shouldShowBluetoothEnableDialog = !isBlueToothAvailable) }
        checkNextShouldEnableOrNot()
    }

    private fun collectBluetoothAvailableState() {
        bluetoothAvailableStateCollector
            .collectState()
            .onEach { state ->
                if (state == BluetoothAvailableState.LOCATION_PERMISSION_NOT_GRANTED) {
                    isBlueToothAvailable = isBlueToothEnabledUseCase()
                }
                isBlueToothAvailable = !(state == BluetoothAvailableState.BLUETOOTH_NOT_AVAILABLE
                        || state == BluetoothAvailableState.BLUETOOTH_NOT_ENABLED)
                checkNextShouldEnableOrNot()
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun checkNextShouldEnableOrNot() {
        val shouldEnable = uiState.value.lockName.isNotEmpty()
                && (uiState.value.adminCode.isNotEmpty() || uiState.value.hasAdminCodeBeenSet)
                && uiState.value.timezone.isNotEmpty()
                && isBlueToothAvailable
        _uiState.update { it.copy(isNextEnable = shouldEnable) }
    }

    fun deleteLock() {
        flow { emit(lock!!.delete(getClientTokenUseCase())) }
            .onEach {
                _uiEvent.emit(AdminCodeUiEvent.DeleteLockSuccess)
                lock?.let {
                    if (it.isConnected())
                        it.disconnect()
                }
            }
            .catch {
                Timber.e(it)
                _uiEvent.emit(AdminCodeUiEvent.DeleteLockFail)
            }
            .launchIn(viewModelScope)
    }

    fun setUsername(name: String) {
        _uiState.update { it.copy(userName = name) }
        checkNextShouldEnableOrNot()
    }

    fun isLockConnected(): Boolean = lock?.isConnected() ?: false
    fun closeErrorDialog() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}

data class AdminCodeUiState(
    val lockName: String = "",
    val userName: String = "",
    val adminCode: String = "",
    val timezone: String = ZoneId.systemDefault().id,
    val isNextEnable: Boolean = false,
    val shouldShowExitDialog: Boolean = false,
    val timezoneList: Array<String> = ZoneId.getAvailableZoneIds()
        .toList()
        .sortedBy { it }
        .toTypedArray(),
    val showTimezoneMenu: Boolean = false,
    val hasAdminCodeBeenSet: Boolean = false,
    val shouldShowBluetoothEnableDialog: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String = ""
)

sealed class AdminCodeUiEvent {
    object DeleteLockSuccess : AdminCodeUiEvent()
    object DeleteLockFail : AdminCodeUiEvent()
    object Success : AdminCodeUiEvent()
    object Failed : AdminCodeUiEvent()
}