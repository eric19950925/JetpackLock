package com.sunion.jetpacklock.account_management

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.jetpacklock.domain.repository.AuthRepository
import com.sunion.jetpacklock.domain.usecase.account.DeleteAccountUseCase
import com.sunion.jetpacklock.domain.usecase.account.GetIdTokenUseCase
import com.sunion.jetpacklock.domain.usecase.account.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sunion.jetpacklock.R

@HiltViewModel
class MemberManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val signOut: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val application: Application,
    private val getIdToken: GetIdTokenUseCase
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _username = mutableStateOf("")
    val username: State<String> = _username

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _deleteAccountConfirmText = mutableStateOf("")
    val deleteAccountConfirmText: State<String> = _deleteAccountConfirmText

    private val _showDeleteAccountAlert = mutableStateOf(false)
    val showDeleteAccountAlert: State<Boolean> = _showDeleteAccountAlert

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private val _alertMessage = mutableStateOf("")
    val alertMessage: State<String> = _alertMessage

    fun loadData() {
        authRepository
            .getName()
            .flowOn(Dispatchers.IO)
            .onEach { _username.value = it ?: "" }
            .catch { e -> Log.e("TAG",e.toString()) }
            .launchIn(viewModelScope)
    }

    fun setDeleteAccountConfirmText(password: String) {
        _deleteAccountConfirmText.value = password
    }

    @OptIn(FlowPreview::class)
    fun deleteAccount() {
//        if (deleteAccountConfirmText.value != "Delete") {
//            viewModelScope.launch {
//                _alertMessage.value =
//                    application.getString(R.string.member_failed_to_delete_account)
//            }
//            return
//        }

        deleteAccountUseCase(deleteAccountConfirmText.value)
            .flowOn(Dispatchers.IO)
            .onStart { _isLoading.value = true }
            .onCompletion { _isLoading.value = false }
            .onEach { viewModelScope.launch { _uiEvent.emit(UiEvent.DeleteAccountSuccess) } }
            .catch { e ->
                Log.e("TAG",e.toString())
                _alertMessage.value =
                    application.getString(R.string.member_failed_to_delete_account)
            }
            .launchIn(viewModelScope)
    }

    fun closeDeleteAccountAlert() {
        _showDeleteAccountAlert.value = false
    }

    fun displayDeleteAccountAlert() {
        _showDeleteAccountAlert.value = true
    }

    fun logOut() {
        signOut()
            .flowOn(Dispatchers.IO)
            .onStart { _isLoading.value = true }
            .onCompletion { _isLoading.value = false }
            .onEach { viewModelScope.launch { _uiEvent.emit(UiEvent.SignOutSuccess) } }
            .catch { e ->
                Log.e("TAG",e.toString())
                viewModelScope.launch { _alertMessage.value = "Failure to log out" }
            }
            .launchIn(viewModelScope)
    }

    fun clearAlertMessage() {
        _alertMessage.value = ""
    }

    sealed class UiEvent {
        object SignOutSuccess : UiEvent()
        object DeleteAccountSuccess : UiEvent()
    }
}
