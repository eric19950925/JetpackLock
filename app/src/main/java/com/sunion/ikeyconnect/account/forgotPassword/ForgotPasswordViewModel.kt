package com.sunion.ikeyconnect.account.forgotPassword

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.domain.usecase.account.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.exception.UserNotFoundException

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val forgotPassword: ForgotPasswordUseCase
) : ViewModel() {
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _email = mutableStateOf(savedStateHandle.get("email") ?: "")
    val email: State<String> = _email

    private val _emailError = mutableStateOf(savedStateHandle.get("emailError") ?: "")
    val emailError: State<String> = _emailError

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    fun setEmail(email: String) {
        _email.value = email
        savedStateHandle["email"] = email
    }

    fun requestForgotPassword() {
        if (email.value.isEmpty()) {
            _emailError.value = application.getString(R.string.account_this_field_is_required)
            return
        }

        forgotPassword(email.value)
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach { viewModelScope.launch { _uiEvent.emit(UiEvent.Success) } }
            .catch { e ->
                _emailError.value = if (e is UserNotFoundException)
                    application.getString(R.string.account_user_does_not_exist)
                else e.message ?: "Error"
                Log.d("TAG","forgotPassword failure: $e")
            }
            .launchIn(viewModelScope)
    }

    sealed class UiEvent {
        object Success : UiEvent()
    }
}