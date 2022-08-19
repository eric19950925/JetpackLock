package com.sunion.ikeyconnect.users

import androidx.lifecycle.ViewModel
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.home.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(private val lockProvider: LockProvider,
private val iotService: SunionIotService,
private val getUuid: GetUuidUseCase,
private val toastHttpException: ToastHttpException,
private val statefulConnection: ReactiveStatefulConnection,
): ViewModel(){
    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState

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
    fun leavePage(onNext: () -> Unit) { onNext.invoke() }
}

data class UsersUiState(
    val isLoading: Boolean = false,
    val deviceType: Int = HomeViewModel.DeviceType.WiFi.typeNum,
)