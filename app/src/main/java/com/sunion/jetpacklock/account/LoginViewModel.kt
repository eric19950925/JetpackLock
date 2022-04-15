package com.sunion.jetpacklock.account

import android.util.Log
import androidx.lifecycle.*
import com.sunion.jetpacklock.domain.usecase.SignInUseCase
import com.sunion.jetpacklock.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    private val _logger = MutableLiveData<String>("Welcome~\n")
    val logger: LiveData<String> = _logger

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    fun login(){
        signIn.invoke(email.value?:"",password.value?:"")
            .flowOn(Dispatchers.IO)
            .onEach {
                viewModelScope.launch {
                    _uiEvent.emit(UiEvent.Success)
                    Log.d("TAG", "login $it.")
                }
            }
            .catch { e ->
                Log.e("TAG", "login failure: $e")
            }
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

    fun setEmail(email: String){
        _email.value = email
    }
    fun setPassword(password: String){
        _password.value = password
    }
    fun cleanLogger(){
        _logger.value = ""
    }
}
sealed class UiEvent {
    object Success : UiEvent()
    data class Fail(val message: String) : UiEvent()
}