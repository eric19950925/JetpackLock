package com.sunion.ikeyconnect.add_lock.scan_qrcode

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.exception.LockAlreadyExistedException
import com.sunion.ikeyconnect.domain.usecase.add_lock.ParsingQRCodeFromImageUriUseCase
import com.sunion.ikeyconnect.domain.usecase.device.SaveLockInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ScanLockQRCodeViewModel @Inject constructor(
    private val parsingQRCodeFromImageUriUseCase: ParsingQRCodeFromImageUriUseCase,
    private val saveLockInfoUseCase: SaveLockInfoUseCase,
    private val application: Application,
    private val lockProvider: LockProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanLockQRCodeUiState())
    val uiState: StateFlow<ScanLockQRCodeUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<ScanLockQRCodeUiEvent>()
    val uiEvent: SharedFlow<ScanLockQRCodeUiEvent> = _uiEvent

    fun setQRCodeContent(content: String) {
        Timber.d(content)

        flow {
            emit(lockProvider.getLockByQRCode(content))
        }
            .onEach {
                if (it == null) {
                    _uiState.update { state -> state.copy(message = "Cannot be paired with the device") }
                    return@onEach
                }

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val lockInfo = saveLockInfoUseCase(it.lockInfo)
                        _uiEvent.emit(ScanLockQRCodeUiEvent.Complete(lockInfo.macAddress))
                    } catch (e: Exception) {
                        Timber.e(e)
                        if (e is LockAlreadyExistedException)
                            _uiState.update { state ->
                                state.copy(
                                    message = application.getString(
                                        R.string.add_lock_already_exist_content
                                    )
                                )
                            }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .catch {
                Timber.e(it)
                _uiState.update { state -> state.copy(message = "Cannot be paired with the device") }
            }
            .launchIn(viewModelScope)
    }


    fun setQRCodeImageUri(uri: Uri?) {
        if (uri == null) {
            return
        }

        runCatching { parsingQRCodeFromImageUriUseCase(uri) }
            .getOrNull()
            ?.let(this::setQRCodeContent)
            ?: let { _uiState.update { it.copy(message = application.getString(R.string.scan_code_error_title)) } }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun closeMessageDialog() {
        _uiState.update { it.copy(message = "") }
    }
}

data class ScanLockQRCodeUiState(val isTorchOn: Boolean = false, val message: String = "")

sealed class ScanLockQRCodeUiEvent {
    data class Complete(val macAddress: String) : ScanLockQRCodeUiEvent()
}