package com.sunion.ikeyconnect.add_lock.lock_overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
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
    private val lockProvider: LockProvider
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
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }
}

data class LockOverviewUiState(
    val lockName: String = "",
    val userName: String = "",
    val location: LatLng? = null
)