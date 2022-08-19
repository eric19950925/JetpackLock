package com.sunion.ikeyconnect.auto_unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.RegistryGetResponse
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.home.HomeViewModel
import com.sunion.ikeyconnect.lock.AllLock
import com.sunion.ikeyconnect.settings.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AutoUnlockViewModel @Inject constructor(
    private val lockProvider: LockProvider,
    private val iotService: SunionIotService,
    private val getUuid: GetUuidUseCase,
    private val toastHttpException: ToastHttpException,
    private val statefulConnection: ReactiveStatefulConnection,
): ViewModel(){
    private val _uiState = MutableStateFlow(AutoUnlockUiState())
    val uiState: StateFlow<AutoUnlockUiState> = _uiState

    private var _deviceIdentity: String? = null
    val deviceIdentity: String?
        get() = _deviceIdentity

    private var _deviceType: Int? = null
    val deviceType: Int?
        get() = _deviceType

    fun init(DeviceIdentity: String, deviceType: Int) {
        _deviceIdentity = DeviceIdentity
        _deviceType = deviceType
        _uiState.update { it.copy(deviceType = deviceType)}

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
}
data class AutoUnlockUiState(
    val isLoading: Boolean = false,
    val deviceType: Int = HomeViewModel.DeviceType.WiFi.typeNum,
)