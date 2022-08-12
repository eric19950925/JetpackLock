package com.sunion.ikeyconnect.add_lock.admin_code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.BluetoothAvailableState
import com.sunion.ikeyconnect.domain.blelock.BluetoothAvailableStateCollector
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.BleLock
import com.sunion.ikeyconnect.domain.model.DeviceToken
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetIdentityIdUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.domain.usecase.device.IsBlueToothEnabledUseCase
import com.sunion.ikeyconnect.domain.usecase.home.*
import com.sunion.ikeyconnect.home.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AdminCodeViewModel @Inject constructor(
    private val isBlueToothEnabledUseCase: IsBlueToothEnabledUseCase,
    private val getClientTokenUseCase: GetClientTokenUseCase,
    private val bluetoothAvailableStateCollector: BluetoothAvailableStateCollector,
    private val lockProvider: LockProvider,
    private val lockInformationRepository: LockInformationRepository,
    private val userSync: UserSyncUseCase,
    private val getIdToken: GetIdTokenUseCase,
    private val getIdentityId: GetIdentityIdUseCase,
    private val getUuid: GetUuidUseCase,
    private val provisionDomain: ProvisionDomain,
    private val toastHttpException: ToastHttpException,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminCodeUiState())
    val uiState: StateFlow<AdminCodeUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<AdminCodeUiEvent>()
    val uiEvent: SharedFlow<AdminCodeUiEvent> = _uiEvent

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress
    private var _deviceType: Int? = null
    val deviceType: Int?
        get() = _deviceType
    private var lock: Lock? = null

    private var isBlueToothAvailable = true
    private var job: Job? = null

    fun init(macAddress: String, deviceType: Int) {
        this._macAddress = macAddress
        this._deviceType = deviceType
        viewModelScope.launch(Dispatchers.IO) {
            lock = lockProvider.getLockByMacAddress(macAddress)
            lock?.let {
                _uiState.update { state ->
                    state.copy(
//                        hasAdminCodeBeenSet = it.hasAdminCodeBeenSet(),
                        shouldShowBluetoothEnableDialog = !isBlueToothEnabledUseCase(),
                        isWiFiLock = deviceType == HomeViewModel.DeviceType.WiFi.typeNum
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
        when(deviceType){
            HomeViewModel.DeviceType.WiFi.typeNum -> {
                job?.cancel()
                job = run {
                    if (state.hasAdminCodeBeenSet)
                        flow { emit(lock.editToken(0, DeviceToken.PERMISSION_ALL, state.userName)) }
                    else
                        flow {
                            emit(
                                lock.changeAdminCode(
                                    provisionDomain.provisionThingName,
                                    state.adminCode,
                                    state.lockName,
                                    state.timezone,
                                    getUuid.invoke()
                                )
                            )
                        }
                }
                    .onStart { _uiState.update { it.copy(isProcessing = true) } }
                    .onCompletion {
                        _uiState.update { it.copy(isProcessing = false) }
                        _uiEvent.emit(AdminCodeUiEvent.Success)
                    }
                    .flowOn(Dispatchers.IO)
                    .catch {
                        Timber.e(it)
                        _uiState.update { state1 -> state1.copy(errorMessage = "Failed to set admin code") }
                    }
                    .launchIn(viewModelScope)
            }
            else -> {
                //C7 E7
                flow { emit( lock.changeAdminCodeByBle(macAddress?: throw Exception("macAddressNull"), state.adminCode, state.userName, getUuid.invoke())) }
                    .onEach { Timber.d("admin code has been set: $it") }
                    .map { lock.changeLockNameByBle(state.lockName) }
                    .map { lock.setTimeZoneByBle(state.timezone) }
                    .onStart { _uiState.update { it.copy(isProcessing = true) } }
                    .onCompletion { _uiState.update { it.copy(isProcessing = false) } }
                    .flowOn(Dispatchers.IO)
                    .catch {
                        Timber.e(it)
                        _uiState.update { state1 -> state1.copy(errorMessage = "Failed to set admin code") }
                    }
                    .launchIn(viewModelScope)
                //userSync
                lockInformationRepository.get(macAddress?:throw Exception("macAddress is null")).toObservable()
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .onEach {
                        if(provisionDomain.provisionThingName == "") {
                            updateUserSyncForBleLock(
                                UserSyncOrder(DeviceIdentity = it.macAddress, DeviceType = if(it.model == "KDW00")"ble mode" else "ble", DisplayName = state.lockName, Order = 0),
                                BleLock(
                                    MACAddress = it.macAddress, DisplayName = state.lockName,
                                    OneTimeToken = it.oneTimeToken, PermanentToken = it.permanentToken,
                                    ConnectionKey = it.keyOne, SharedFrom = it.sharedFrom?:"owner"
                                ),
                            )
                        }
                    }
                    .flowOn(Dispatchers.IO)
                    .catch {
                        Timber.e(it)
                        toastHttpException(it)
                    }
                    .launchIn(viewModelScope)
            }
        }
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

    private fun updateUserSyncForBleLock(newLockInfo: UserSyncOrder, bleLock: BleLock){
        flow { emit(userSync.getUserSync(getUuid.invoke())) }
            .map {
                val orderData = it.Payload.Dataset.DeviceOrder
                val bleLockData = it.Payload.Dataset.BLEDevices
                (orderData to bleLockData)
            }
            .map { (orderData, bleLockData) ->
                val newOrderList = mutableListOf<UserSyncOrder>()
                val newBleLockList = mutableListOf<BleLock>()
                orderData?.Order?.let { newOrderList.addAll(it) }
                bleLockData?.Devices?.let { newBleLockList.addAll(it) }
                newLockInfo.let { newOrderList.add(it) }
                bleLock.let { newBleLockList.add(it) }
                userSync.updateUserSync(getUuid.invoke(), UserSyncRequestPayload(
                    Dataset = RequestDataset(
                        DeviceOrder = RequestOrder(newOrderList, orderData?.version?:0),
                        BLEDevices = RequestDevices(newBleLockList, bleLockData?.version?:0)
                    )
                )
                )
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                Timber.d("updateUserSyncForBleLock")
                _uiState.update { it.copy(isLoading = true) } }
            .onCompletion {
                delay(1000)
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(AdminCodeUiEvent.Success)
            }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
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
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val isWiFiLock: Boolean = false,
)

sealed class AdminCodeUiEvent {
    object DeleteLockSuccess : AdminCodeUiEvent()
    object DeleteLockFail : AdminCodeUiEvent()
    object Success : AdminCodeUiEvent()
    object Failed : AdminCodeUiEvent()
}