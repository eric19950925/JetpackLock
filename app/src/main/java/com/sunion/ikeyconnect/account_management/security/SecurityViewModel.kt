package com.sunion.ikeyconnect.account_management.security

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.usecase.account.ChangePasswordUseCase
import com.sunion.ikeyconnect.domain.usecase.account.SignOutAllDeviceUseCase
import com.sunion.ikeyconnect.domain.usecase.account.SignOutUseCase
import com.sunion.ikeyconnect.domain.usecase.account.VerifyPasswordStrengthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val application: Application,
    private val signOut: SignOutUseCase,
    private val signOutAllDevice: SignOutAllDeviceUseCase,
    private val verifyPasswordStrength: VerifyPasswordStrengthUseCase,
    private val changePassword: ChangePasswordUseCase
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _currentPassword = mutableStateOf("")
    val currentPassword: State<String> = _currentPassword

    private val _currentPasswordError = mutableStateOf("")
    val currentPasswordError: State<String> = _currentPasswordError

    private val _newPassword = mutableStateOf("")
    val newPassword: State<String> = _newPassword

    private val _newPasswordError = mutableStateOf("")
    val newPasswordError: State<String> = _newPasswordError

    private val _showPasswordTips = mutableStateOf(false)
    val showPasswordTips: State<Boolean> = _showPasswordTips

    private val _confirmNewPassword = mutableStateOf("")
    val confirmNewPassword: State<String> = _confirmNewPassword

    private val _confirmNewPasswordError = mutableStateOf("")
    val confirmNewPasswordError: State<String> = _confirmNewPasswordError

    private val _showLogOutAllDeviceAlert = mutableStateOf(false)
    val showLogOutAllDeviceAlert: State<Boolean> = _showLogOutAllDeviceAlert

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private val _saveSuccess = mutableStateOf<Boolean?>(null)
    val saveSuccess: State<Boolean?> = _saveSuccess

    private val errorFieldIsRequired =
        application.getString(R.string.account_this_field_is_required)

    fun setCurrentPassword(password: String) {
        _currentPassword.value = password
    }

    fun setNewPassword(password: String) {
        _showPasswordTips.value = false

        _newPassword.value = password

        if (!verifyPasswordStrength(password))
            _showPasswordTips.value = true
    }

    fun setConfirmNewPassword(password: String) {
        _confirmNewPasswordError.value = ""

        _confirmNewPassword.value = password

        if (newPassword.value != password)
            _confirmNewPasswordError.value =
                application.getString(R.string.account_password_does_not_match)
    }

    fun changePasswordToRemote() {
        if (currentPassword.value.isEmpty()) {
            _currentPasswordError.value = errorFieldIsRequired
            return
        }
        if (newPassword.value.isEmpty()) {
            _newPasswordError.value = errorFieldIsRequired
            return
        }
        if (confirmNewPassword.value.isEmpty()) {
            _confirmNewPasswordError.value = errorFieldIsRequired
            return
        }
        if (newPassword.value != confirmNewPassword.value) {
            _confirmNewPasswordError.value =
                application.getString(R.string.account_password_does_not_match)
            return
        }

        changePassword(currentPassword.value, newPassword.value)
            .flowOn(Dispatchers.IO)
            .onStart { _isLoading.value = true }
            .onCompletion { _isLoading.value = false }
            .onEach { viewModelScope.launch { _saveSuccess.value = true } }
            .catch { viewModelScope.launch { _saveSuccess.value = false } }
            .launchIn(viewModelScope)

    }

    fun displayLogOutAllDeviceAlert() {
        _showLogOutAllDeviceAlert.value = true
    }

    fun closeLogOutAllDeviceAlert() {
        _showLogOutAllDeviceAlert.value = false
    }

    fun logOutAllDevice() {

        signOutAllDevice()
            .flowOn(Dispatchers.IO)
            .onStart { _isLoading.value = true }
            .onCompletion { _isLoading.value = false }
            .onEach { viewModelScope.launch { _uiEvent.emit(UiEvent.SignOutAllSuccess) } }
            .catch { viewModelScope.launch { _uiEvent.emit(UiEvent.SignOutAllFail("")) } }
            .launchIn(viewModelScope)

    }

    fun logOut(){
        signOut.invoke()
            .flowOn(Dispatchers.IO)
            .onEach {
                viewModelScope.launch {
                    Log.d("TAG", "logout success.")
                }
            }
            .catch { e ->
                Log.d("TAG","logOut failure: $e")
            }
            .launchIn(viewModelScope)
    }

    fun clearSaveResult() {
        _saveSuccess.value = null
    }

    sealed class UiEvent {
        object SignOutAllSuccess : UiEvent()
        data class SignOutAllFail(val message: String) : UiEvent()
    }
}