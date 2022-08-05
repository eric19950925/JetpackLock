package com.sunion.ikeyconnect.add_lock.request_location

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RequestLocationViewModel @Inject constructor(
    private val application: Application,
    private val getClientTokenUseCase: GetClientTokenUseCase,
    private val lockProvider: LockProvider,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<RequestLocationUiEvent>()
    val uiEvent: SharedFlow<RequestLocationUiEvent> = _uiEvent

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress

    fun deleteLock() {
        val mac = macAddress ?: return
        flow { emit(lockProvider.getLockByMacAddress(mac)!!.delete(getClientTokenUseCase())) }
            .onEach { _uiEvent.emit(RequestLocationUiEvent.DeleteLockSuccess) }
            .catch {
                Timber.e(it)
                _uiEvent.emit(RequestLocationUiEvent.DeleteLockFail)
            }
            .launchIn(viewModelScope)
    }

    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        application, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
        application, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun init(macAddress: String) {
        _macAddress = macAddress
    }
}


sealed class RequestLocationUiEvent {
    object DeleteLockSuccess : RequestLocationUiEvent()
    object DeleteLockFail : RequestLocationUiEvent()
}