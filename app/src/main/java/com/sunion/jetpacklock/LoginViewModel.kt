package com.sunion.jetpacklock

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    fun login(){
        signIn.invoke(email.value?:"",password.value?:"")
            .flowOn(Dispatchers.IO)
            .onEach {
                viewModelScope.launch {
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