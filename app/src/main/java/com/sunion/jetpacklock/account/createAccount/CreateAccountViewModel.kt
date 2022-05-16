package com.sunion.jetpacklock.account.createAccount

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.jetpacklock.domain.usecase.account.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.domain.exception.UsernameException
import kotlinx.coroutines.flow.*
import java.util.concurrent.CancellationException

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val verifyPasswordStrength: VerifyPasswordStrengthUseCase,
    private val checkIfUserExists: CheckIfUserExistsUseCase,
    private val signUp: SignUpUseCase,
    private val resendSignUpConfirmCode: ResendSignUpConfirmCodeUseCase,
    private val confirmSignUp: ConfirmSignUpUseCase
) : ViewModel() {
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _email = mutableStateOf(savedStateHandle.get(EMAIL) ?: "")
    val email: State<String> = _email

    private val _emailError = mutableStateOf(savedStateHandle.get(EMAIL_ERROR) ?: "")
    val emailError: State<String> = _emailError

    private val _password = mutableStateOf(savedStateHandle.get(PASSWORD) ?: "")
    val password: State<String> = _password

    private val _passwordError = mutableStateOf(savedStateHandle.get(PASSWORD_ERROR) ?: "")
    val passwordError: State<String> = _passwordError

    private val _passwordConfirm = mutableStateOf(savedStateHandle.get(PASSWORD_CONFIRM) ?: "")
    val passwordConfirm: State<String> = _passwordConfirm

    private val _passwordConfirmError =
        mutableStateOf(savedStateHandle.get(PASSWORD_CONFIRM_ERROR) ?: "")
    val passwordConfirmError: State<String> = _passwordConfirmError

    private val _showPasswordTips = mutableStateOf(false)
    val showPasswordTips: State<Boolean> = _showPasswordTips

    private val _verificationCode = mutableStateOf(savedStateHandle.get(VERIFICATION_CODE) ?: "")
    val signUpConfirmCode: State<String> = _verificationCode

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private val errorFieldIsRequired =
        application.getString(R.string.account_this_field_is_required)

    fun setEmail(email: String) {
        _email.value = email
        savedStateHandle[EMAIL] = email
    }

    fun checkEmail() {
        _emailError.value = ""
        if (email.value.isEmpty()) {
            _emailError.value = errorFieldIsRequired
            savedStateHandle[EMAIL_ERROR] = errorFieldIsRequired
            return
        }

        checkIfUserExists(email.value)
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach { exists ->
                if (exists)
                    _emailError.value =
                        application.getString(R.string.account_email_has_been_used)
                else
                    viewModelScope.launch { _uiEvent.emit(UiEvent.GoPasswordScreen) }
            }
            .catch { e ->
                e.message?.let { _emailError.value = it }
                Log.e("TAG",e.toString())
            }
            .launchIn(viewModelScope)
    }

    fun setPassword(password: String) {
        _showPasswordTips.value = false

        _password.value = password
        savedStateHandle[PASSWORD] = password

        if (!verifyPasswordStrength(this.password.value))
            _showPasswordTips.value = true
    }

    fun setPasswordConfirm(password: String) {
        _passwordConfirmError.value = ""

        _passwordConfirm.value = password
        savedStateHandle[PASSWORD_CONFIRM] = password

        if (this.password.value != password) {
            val message = application.getString(R.string.account_password_does_not_match)
            _passwordConfirmError.value = message
        }
    }

    fun checkPassword() {
        _passwordError.value = ""
        _passwordConfirmError.value = ""
        if (password.value.isEmpty()) {
            _passwordError.value = errorFieldIsRequired
            savedStateHandle[PASSWORD_ERROR] = errorFieldIsRequired
            return
        }
        if (passwordConfirm.value.isEmpty()) {
            _passwordConfirmError.value = errorFieldIsRequired
            savedStateHandle[PASSWORD_CONFIRM_ERROR] = errorFieldIsRequired
            return
        }
        if (!verifyPasswordStrength(password.value)) {
            _showPasswordTips.value = true
            return
        }
        if (password.value != passwordConfirm.value) {
            val message = application.getString(R.string.account_password_does_not_match)
            _passwordConfirmError.value = message
            savedStateHandle[PASSWORD_CONFIRM_ERROR] = message
            return
        }

        signUp(email.value, password.value)
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach {
                viewModelScope.launch { _uiEvent.emit(UiEvent.GoValidateScreen) }
            }
            .catch { e ->
                Log.e("TAG",e.toString())
                val err = if (e is CancellationException) e.cause else e
                if (err is UsernameException)
                    err.message?.let { _emailError.value = it }
                else
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.SignUpFail(e.message ?: "SignUp error"))
                    }
            }
            .launchIn(viewModelScope)
    }

    fun setVerificationCode(code: String) {
        _verificationCode.value = code
        savedStateHandle[VERIFICATION_CODE] = code
    }

    fun resendVerificationCode() {
        resendSignUpConfirmCode(email.value)
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach {
                viewModelScope.launch { _uiEvent.emit(UiEvent.ConfirmCodeHasBeenResend) }
            }
            .catch { e ->
                val err = if (e is CancellationException) e.cause else e
                if (err is UsernameException)
                    err.message?.let { _emailError.value = it }
                else
                    viewModelScope.launch {
                        _uiEvent.emit(
                            UiEvent.ResendSignUpConfirmCodeFail(
                                e.message ?: "ResendSignUpConfirmationCode error"
                            )
                        )
                    }
            }
            .launchIn(viewModelScope)
    }

    fun checkVerificationCode() {
        confirmSignUp(email.value, signUpConfirmCode.value)
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach {
                viewModelScope.launch { _uiEvent.emit(UiEvent.GoSuccessScreen) }
            }
            .catch { e ->
                val err = if (e is CancellationException) e.cause else e
                if (err is UsernameException)
                    err.message?.let { _emailError.value = it }
                else
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.SignUpFail(e.message ?: "ConfirmSignUp error"))
                    }
            }
            .launchIn(viewModelScope)
    }

    sealed class UiEvent {
        object GoPasswordScreen : UiEvent()
        object GoValidateScreen : UiEvent()
        object GoSuccessScreen : UiEvent()
        data class SignUpFail(val message: String) : UiEvent()
        object ConfirmCodeHasBeenResend : UiEvent()
        data class ResendSignUpConfirmCodeFail(val message: String) : UiEvent()
    }

    companion object {
        const val EMAIL = "email"
        const val EMAIL_ERROR = "emailError"
        const val PASSWORD = "password"
        const val PASSWORD_ERROR = "passwordError"
        const val PASSWORD_CONFIRM = "passwordConfirm"
        const val PASSWORD_CONFIRM_ERROR = "passwordConfirmError"
        const val VERIFICATION_CODE = "verificationCode"
    }
}