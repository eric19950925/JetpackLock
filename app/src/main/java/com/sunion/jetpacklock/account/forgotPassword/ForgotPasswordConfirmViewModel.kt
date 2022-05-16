package com.sunion.jetpacklock.account.forgotPassword

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.jetpacklock.domain.usecase.account.ConfirmForgotPasswordUseCase
import com.sunion.jetpacklock.domain.usecase.account.ForgotPasswordUseCase
import com.sunion.jetpacklock.domain.usecase.account.VerifyPasswordStrengthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.domain.exception.IKeyException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*


@HiltViewModel
class ForgotPasswordConfirmViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val confirmForgotPassword: ConfirmForgotPasswordUseCase,
    private val forgotPassword: ForgotPasswordUseCase,
    private val verifyPasswordStrength: VerifyPasswordStrengthUseCase
) : ViewModel() {
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _confirmCode = mutableStateOf(savedStateHandle.get("confirmCode") ?: "")
    val confirmCode: State<String> = _confirmCode

    private val _confirmCodeError = mutableStateOf(savedStateHandle.get("confirmCodeError") ?: "")
    val confirmCodeError: State<String> = _confirmCodeError

    private val _newPassword = mutableStateOf(savedStateHandle.get("newPassword") ?: "")
    val newPassword: State<String> = _newPassword

    private val _newPasswordError = mutableStateOf(savedStateHandle.get("newPasswordError") ?: "")
    val newPasswordError: State<String> = _newPasswordError

    private val _showPasswordTips = mutableStateOf(false)
    val showPasswordTips: State<Boolean> = _showPasswordTips

    private val _confirmPassword = mutableStateOf(savedStateHandle.get("confirmPassword") ?: "")
    val confirmPassword: State<String> = _confirmPassword

    private val _confirmPasswordError = mutableStateOf(savedStateHandle.get("") ?: "")
    val confirmPasswordError: State<String> = _confirmPasswordError

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private var _email = savedStateHandle.get("email") ?: ""
    val email: String
        get() = _email

    private val errorFieldIsRequired =
        application.getString(R.string.account_this_field_is_required)

    fun setConfirmCode(confirmCode: String) {
        _confirmCode.value = confirmCode
        savedStateHandle["confirmCode"] = confirmCode
    }

    fun setNewPassword(newPassword: String) {
        _showPasswordTips.value = false

        _newPassword.value = newPassword
        savedStateHandle["newPassword"] = newPassword

        if (!verifyPasswordStrength(newPassword))
            _showPasswordTips.value = true
    }

    fun setConfirmPassword(confirmPassword: String) {
        _confirmPasswordError.value = ""

        _confirmPassword.value = confirmPassword
        savedStateHandle["confirmPassword"] = confirmPassword

        if (newPassword.value != confirmPassword)
            _confirmPasswordError.value =
                application.getString(R.string.account_password_does_not_match)
    }

    fun resendConfirmCode() {
        if (email.isEmpty())
            return
        forgotPassword(email)
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach { }
            .catch { e ->
                val err = if (e is CancellationException) e.cause else e
                if (err is IKeyException)
                    err.message?.let { _confirmCodeError.value = it }
                else
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.Failed(e.message ?: "forgotPassword error"))
                    }
            }
            .launchIn(viewModelScope)
    }

    fun confirmForgotPassword() {
        if (confirmCode.value.isEmpty()) {
            _confirmCodeError.value = errorFieldIsRequired
            return
        }
        if (newPassword.value.isEmpty()) {
            _newPasswordError.value = errorFieldIsRequired
            return
        }
        if (confirmPassword.value.isEmpty()) {
            _confirmPasswordError.value = errorFieldIsRequired
            return
        }
        if (newPassword.value != confirmPassword.value) {
            _confirmPasswordError.value =
                application.getString(R.string.account_password_does_not_match)
            return
        }

        confirmForgotPassword(newPassword.value, confirmCode.value)
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach { viewModelScope.launch { _uiEvent.emit(UiEvent.Success) } }
            .catch { e ->
                val err = if (e is CancellationException) e.cause else e
                if (err is IKeyException)
                    err.message?.let { _confirmCodeError.value = it }
                else
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.Failed(e.message ?: "confirmForgotPassword error"))
                    }
            }
            .launchIn(viewModelScope)
    }

    fun setEmail(email: String) {
        _email = email
        savedStateHandle["email"] = email
    }

    sealed class UiEvent {
        object Success : UiEvent()
        data class Failed(val message: String) : UiEvent()
    }
}