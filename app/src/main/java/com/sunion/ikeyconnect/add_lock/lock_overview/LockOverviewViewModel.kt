package com.sunion.ikeyconnect.add_lock.lock_overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.BleLock
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetIdentityIdUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.domain.usecase.home.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
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
) :
    ViewModel() {
    private val _uiState = MutableStateFlow(LockOverviewUiState())
    val uiState: StateFlow<LockOverviewUiState> = _uiState

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress

    @OptIn(FlowPreview::class)
    fun init(macAddress: String) {
        _macAddress = macAddress
//        flow { emit(lockProvider.getLockByMacAddress(macAddress)) }
//            .flatMapConcat { flow { emit(it!!.getLockConfig()) } }
//            .flowOn(Dispatchers.IO)
//            .onEach {
//                if (it.latitude != null && it.longitude != null) {
//                    Timber.d("LockOverview - latitude:${it.latitude}, longitude:${it.longitude}")
//                    _uiState.update { state ->
//                        state.copy(location = LatLng(it.latitude!!, it.longitude!!))
//                    }
//                }
//            }
//            .catch { Timber.e(it) }
//            .launchIn(viewModelScope)

        lockInformationRepository.get(macAddress).toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .onEach {
                Timber.d(it.toString())
                _uiState.update { state ->
                    state.copy(
                        lockName = it.displayName,
                        userName = it.tokenName.takeIf { s -> s.isNotBlank() } ?: "New User"
                    )
                }
                if(provisionDomain.provisionThingName == "") {
                    updateUserSyncForBleLock(
                        UserSyncOrder(DeviceIdentity = it.macAddress, DeviceType = if(it.model == "KDW00")"ble mode" else "ble", DisplayName = it.displayName, Order = 0),
                        BleLock(
                            MACAddress = it.macAddress, DisplayName = it.displayName,
                            OneTimeToken = it.oneTimeToken, PermanentToken = it.permanentToken,
                            ConnectionKey = it.keyOne, SharedFrom = it.sharedFrom?:""
                        ),
                    )
                }
            }

    }
    private fun getOrderList(){
        flow { emit(getIdToken().single()) }
            .map { idToken ->
                val identityId = getIdentityId().single()
                (idToken to identityId)
            }
            .map { (idToken, identityId) ->
                userSync.pubGetUserSync(idToken, identityId, getUuid.invoke())
            }
            .flowOn (Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion {
//                delay(1000)
//                _uiState.update { it.copy(isLoading = false) }
            }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }

    private fun updateUserSyncForBleLock(newLockInfo: UserSyncOrder, bleLock: BleLock){
        flow { emit(userSync.getUserSync(getUuid.invoke())) }
            .map {
                val orderData = it.Payload.Dataset.DeviceOrder
                val bleLockData = it.Payload.Dataset.BLEDevices
                (orderData to bleLockData)
            }
            .map { (orderData, bleLockData) ->
                val newOrderList: MutableList<UserSyncOrder>? = null
                val newBleLockList: MutableList<BleLock>? = null
                orderData?.Order?.let { newOrderList?.addAll(it) }
                bleLockData?.Devices?.let { newBleLockList?.addAll(it) }
                newLockInfo.let { newOrderList?.add(it) }
                bleLock.let { newBleLockList?.add(it) }
                userSync.updateUserSync(getUuid.invoke(), UserSyncRequestPayload(
                    Dataset = RequestDataset(
                        DeviceOrder = RequestOrder(newOrderList?:throw Exception("DeviceOrder is null."), orderData?.version?:0),
                        BLEDevices = RequestDevices(newBleLockList?:throw Exception("BLEDevices is null."), bleLockData?.version?:0)
                    )
                ))
            }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion {
                delay(1000)
                _uiState.update { it.copy(isLoading = false) }
            }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }
}

data class LockOverviewUiState(
    val lockName: String = "",
    val userName: String = "",
    val isLoading: Boolean = false,
    val location: LatLng? = null
)