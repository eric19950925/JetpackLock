package com.sunion.ikeyconnect.add_lock.lock_overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.domain.Interface.AuthRepository
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetIdentityIdUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.domain.usecase.home.*
import com.sunion.ikeyconnect.home.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LockOverviewViewModel @Inject constructor(
    private val lockInformationRepository: LockInformationRepository,
    private val lockProvider: LockProvider,
    private val userSync: UserSyncUseCase,
    private val getIdToken: GetIdTokenUseCase,
    private val getIdentityId: GetIdentityIdUseCase,
    private val getUuid: GetUuidUseCase,
    private val provisionDomain: ProvisionDomain,
    private val toastHttpException: ToastHttpException,
    private val authRepository: AuthRepository,
) :
    ViewModel() {
    private val _uiState = MutableStateFlow(LockOverviewUiState())
    val uiState: StateFlow<LockOverviewUiState> = _uiState

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress

    private var _deviceType: Int? = null
    val deviceType: Int?
        get() = _deviceType

    @OptIn(FlowPreview::class)
    fun init(macAddress: String, deviceType: Int) {
        _macAddress = macAddress
        _deviceType = deviceType
        flow { emit(lockProvider.getLockByMacAddress(macAddress)) }
            .flatMapConcat {
                if(deviceType == HomeViewModel.DeviceType.WiFi.typeNum){
                    Timber.d("LockOverviewViewModel - getLockConfig")
                    flow { emit(it!!.getLockConfig(provisionDomain.provisionThingName, getUuid.invoke())) }
                }
                else {
                    flow { emit(it!!.getLockConfigByBle()) }
                }
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                if (it.latitude != null && it.longitude != null) {
                    Timber.d("LockOverview - latitude:${it.latitude}, longitude:${it.longitude}")
                    _uiState.update { state ->
                        state.copy(location = LatLng(it.latitude, it.longitude))
                    }
                }
            }
            .onStart { _uiState.update { state -> state.copy(isLoading = true) } }
            .onCompletion { _uiState.update { state -> state.copy(isLoading = false) } }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

        flow { emit(userSync.getUserSync(getUuid.invoke())) }
            .map {
                it.Payload.Dataset.DeviceOrder
            }
            .map { deviceOrder ->
                if(deviceType == HomeViewModel.DeviceType.WiFi.typeNum){
                    deviceOrder?.Order?.find { it.DeviceIdentity == provisionDomain.provisionThingName }?.DisplayName
                }
                else deviceOrder?.Order?.find { it.DeviceIdentity == macAddress }?.DisplayName
            }
            .map { displayName ->
                _uiState.update { state ->
                    state.copy(
                        lockName = displayName ?: "",
                        userName = authRepository.getName().singleOrNull() ?: ""
                    )
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { state -> state.copy(isLoading = true) } }
            .onCompletion { _uiState.update { state -> state.copy(isLoading = false) } }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)


        lockInformationRepository
            .get(macAddress)
            .map { lock ->
                lockInformationRepository.delete(lock)
//                    .andThen(userCodeRepository.deleteAll(lockInfo.macAddress))
//                    .andThen(eventLogRepository.deleteAll(lockInfo.macAddress))
//                    .andThen(Single.just(lock.thingName ?: ""))
            }
            .toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

        provisionDomain.provisionThingName = ""
    }
}

data class LockOverviewUiState(
    val lockName: String = "",
    val userName: String = "",
    val isLoading: Boolean = false,
    val location: LatLng? = null
)