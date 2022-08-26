package com.sunion.ikeyconnect.auto_unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.DeviceToken
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.EventState
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.domain.usecase.device.LockSettingUseCase
import com.sunion.ikeyconnect.floorSix
import com.sunion.ikeyconnect.home.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AutoUnlockViewModel @Inject constructor(
    private val lockProvider: LockProvider,
    private val iotService: SunionIotService,
    private val getUuid: GetUuidUseCase,
    private val toastHttpException: ToastHttpException,
    private val statefulConnection: ReactiveStatefulConnection,
    private val lockSettingUseCase: LockSettingUseCase,
    private val autoUnlockUseCase: AutoUnlockUseCase,
    private val lockInformationRepository: LockInformationRepository,
    private val geofenceTaskUseCase: PendingGeofenceTaskUseCase,
): ViewModel(){
    private val _uiState = MutableStateFlow(AutoUnlockUiState())
    val uiState: StateFlow<AutoUnlockUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<AutoUnlockUiEvent>()
    val uiEvent: SharedFlow<AutoUnlockUiEvent> = _uiEvent

    private var _deviceIdentity: String? = null
    val deviceIdentity: String?
        get() = _deviceIdentity

    private var _deviceType: Int? = null
    val deviceType: Int?
        get() = _deviceType

    private val _lockGeoInformation = MutableSharedFlow<Event<Triple<LatLng, String, PendingGeofenceTask>>>()
    val lockGeoInformation: SharedFlow<Event<Triple<LatLng, String, PendingGeofenceTask>>>
        get() = _lockGeoInformation

    val autoUnlockPermissionResult = MutableSharedFlow<Event<Boolean>>()



    private var location = LatLng(25.0330, 121.5654)

    private var lock: Lock? = null

    fun init(DeviceIdentity: String, deviceType: Int) {
        _deviceIdentity = DeviceIdentity
        statefulConnection.connectMacAddress = DeviceIdentity
        _deviceType = deviceType
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

        prepareInfoForUI()
        initLocationInfo()
        collectPermissionResult()
        collectGeofenceOperationResult()
    }

    private fun collectGeofenceOperationResult() {
        viewModelScope.launch {
            geofenceTaskUseCase.GeofenceTask.collect { event ->
                Timber.d(event.toString())
                val data = event.getContentIfNotHandled()
                when (event.status) {
                    EventState.LOADING -> {
                    }
                    EventState.ERROR -> {
                        _uiState.update { it.copy(isAutoUnLockClickable = true)}
                        _uiEvent.emit(AutoUnlockUiEvent.SetAutoUnlockFail)
                    }
                    EventState.SUCCESS -> {
                        _uiState.update { it.copy(isAutoUnLockClickable = true)}
                        data?.let { result ->
                            Timber.d("Auto unlock ${result.second} operation is successful: ${result.first}")
                            if (result.second is PendingGeofenceTask.ADD && result.first) {
                                autoUnlockUseCase
                                    .editAutoUnlockSetting(true, deviceType?:throw Exception("DeviceTypeNull"))
                                    .map {
                                        _uiState.update { it.copy(isAutoUnLockChecked = true)}
                                    }
                                    .flowOn(Dispatchers.IO)
                                    .launchIn(viewModelScope)
                            } else if (result.second is PendingGeofenceTask.REMOVE && result.first) {
                                autoUnlockUseCase
                                    .editAutoUnlockSetting(false, deviceType?:throw Exception("DeviceTypeNull"))
                                    .map {
                                        _uiState.update { it.copy(isAutoUnLockChecked = false)}
                                    }
                                    .flowOn(Dispatchers.IO)
                                    .launchIn(viewModelScope)
                            } else {
                                Timber.e("PendingGeofenceTask NONE")
                            }
                        }
                    }
                }

            }
        }
    }

    fun getDeviceModel(deviceIdentity: String){
        //for fqa url
    }

    private fun initLocationInfo(){
        if ( deviceType == HomeViewModel.DeviceType.WiFi.typeNum ){

        } else {
            flow { emit(lockSettingUseCase.queryConfig().toObservable().asFlow().single()) }
                .map { config ->
                    LatLng(config.latitude ?: 0.0, config.longitude ?: 0.0) to (deviceIdentity?:throw Exception("deviceIdentityNull"))
                }
                .map { pair ->
                    Timber.d("geo info: ${pair.first}, ${pair.second}")
                    _uiState.update { it.copy(initLocation = pair.first) }
                    lockInformationRepository.get(deviceIdentity?:throw Exception("deviceIdentityNull")).blockingGet()
                }
                .map { LockInfo ->
                    _uiState.update { it.copy(isAutoUnLockChecked = LockInfo.isAutoUnlockOn) }
                }
                .catch {
                    Timber.d(it)
                }
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .onCompletion { _uiState.update { it.copy(isLoading = false) } }
                .flowOn(Dispatchers.IO)
                .launchIn(viewModelScope)
        }
    }

    fun getLockGeoInformation(operation: PendingGeofenceTask, deviceIdentity: String?) {
        // use mac address as Request Id when add/remove Geofence
        if ( deviceType == HomeViewModel.DeviceType.WiFi.typeNum ){

        } else {
            flow { emit(lockSettingUseCase.queryConfig().toObservable().asFlow().single()) }
                .map { config ->
                    LatLng(config.latitude ?: 0.0, config.longitude ?: 0.0) to (deviceIdentity?:throw Exception("deviceIdentityNull"))
                }
                .map{ pair ->
                    Timber.d("geo info: ${pair.first}, ${pair.second}")
                    _uiState.update { it.copy(initLocation = pair.first) }
                    _lockGeoInformation.emit(Event.success(Triple(pair.first, pair.second, operation)))
                }
                .catch {
                    Timber.d(it)
                    _lockGeoInformation.emit(Event.error(it::class.java.simpleName))
                }
                .flowOn(Dispatchers.IO)
                .launchIn(viewModelScope)
        }
    }

    private fun collectPermissionResult() {
        autoUnlockPermissionResult
            .onEach { event ->
                val data = event.getContentIfNotHandled()
                when (event.status) {
                    EventState.SUCCESS -> {
                        data?.let { isGranted ->
                            _uiState.update { it.copy(phonePermission = isGranted)}
                            _uiState.update { it.copy(isAutoUnLockClickable = true)}
                        }
                    }
                }
            }
            .catch { Timber.d(it) }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun prepareInfoForUI() {
        if(deviceType?.equals(HomeViewModel.DeviceType.WiFi.typeNum) == true){
            //
        }else {
            if (statefulConnection.isConnectedWithDevice()) {
                statefulConnection.connectionState.map { connectionValue ->
                    statefulConnection.connectMacAddress?.let { _uiState.update { it.copy(lockPermission = connectionValue.data?.second ?: DeviceToken.PERMISSION_NONE)} } ?: kotlin.run {
                        Timber.d("Skip execution, no connection")
                    }
                }
            }
        }

    }

    fun leavePage(onNext: () -> Unit) { onNext.invoke()

//        if(
//            uiState.value.registryAttributes.autoLock == newRegistryAttributes.value?.autoLock &&

//        ){
//            Timber.d(newRegistryAttributes.value.toString())
//            onNext()
//        }
//        else if (newRegistryAttributes.value == null){
//            //disconnect wifi
//            onNext()
//        }
//        else {
//            Timber.d(newRegistryAttributes.value.toString())
//            if(deviceType?.equals(HomeViewModel.DeviceType.WiFi.typeNum) == true){
//                flow { emit(iotService.updateDeviceRegistry(
//                    deviceIdentity?:throw Exception("deviceIdentity is null"),
//                    newRegistryAttributes.value?:throw Exception("newRegistryAttributes is null"),
//                    getUuid.invoke()
//                )) }
//                    .flowOn(Dispatchers.IO)
//                    .onStart { _uiState.update { it.copy(isLoading = true) } }
//                    .onCompletion { _uiState.update { it.copy(isLoading = false) } ; onNext()}
//                    .catch { e ->
//                        toastHttpException(e)
//                    }
//            }else{
//                flow { emit(lockProvider.getLockByMacAddress(deviceIdentity?:throw Exception("macAddressNull"))) }
//                    .map {
//                        val config = (it as AllLock).getLockConfigByBle()
//                        (it as AllLock).setConfig(
//                            config.copy(
//                                isVacationModeOn = newRegistryAttributes.value?.vacationMode?:false,
//                                autoLockTime = newRegistryAttributes.value?.autoLockDelay?:1,
//                                isAutoLock = newRegistryAttributes.value?.autoLock?:false,
//                                isPreamble = newRegistryAttributes.value?.preamble?:false,
//                                isSoundOn = newRegistryAttributes.value?.keyPressBeep?:false,
//                            )
//                        )
//                    }
//                    .flowOn(Dispatchers.IO)
//                    .onStart { _uiState.update { it.copy(isLoading = true) } }
//                    .onCompletion { _uiState.update { it.copy(isLoading = false) } ; onNext()}
//                    .catch { e ->
//                        toastHttpException(e)
//                    }.launchIn(viewModelScope)
//            }

//        }
    }

    fun onAutoUnLockClick() {
        _uiState.update { it.copy(isAutoUnLockClickable = false) }
    }

    fun setLocation(location: LatLng) {
        this.location = location
    }

}
data class AutoUnlockUiState(
    val isLoading: Boolean = false,
    val deviceType: Int = HomeViewModel.DeviceType.WiFi.typeNum,
    val lockPermission: String = DeviceToken.PERMISSION_ALL,
    val phonePermission: Boolean = false,
    val isAutoUnLockChecked: Boolean = false,
    val isAutoUnLockClickable: Boolean = true,
    val initLocation: LatLng = LatLng(25.0330, 121.5654),
)

sealed class AutoUnlockUiEvent {
    object SetAutoUnlockFail : AutoUnlockUiEvent()
    object SetAutoUnlockSuccess : AutoUnlockUiEvent()
}