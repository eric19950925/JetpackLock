package com.sunion.ikeyconnect.account

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.sunion.ikeyconnect.MqttStatefulConnection
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.exception.UsernameException
import com.sunion.ikeyconnect.domain.exception.UsernameExistsException
import com.sunion.ikeyconnect.domain.usecase.account.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val signIn: SignInUseCase,
    private val signOut: SignOutUseCase,
    private val getIdToken: GetIdTokenUseCase,
    private val setFCMUseCase: SetFCMUseCase,
    private val getTimeUseCase: GetTimeUseCase,
    private val isSignedIn: IsSignedInUseCase,
    private val attachPolicyUseCase: AttachPolicyUseCase,
    private val shareInvitationUseCase: ShareInvitationUseCase,
    private val getUserStateDetails: UserStateDetailsUseCase,
    private val cachingCredentialsProvider: CognitoCachingCredentialsProvider,
    private val mqttStatefulConnection: MqttStatefulConnection,
    ) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        Timber.tag("LoginViewModel").d("onCleared")
    }
    private val _logger = MutableLiveData<String>("Welcome~\n")
    val logger: LiveData<String> = _logger

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _email = mutableStateOf(savedStateHandle.get("email") ?: "")
    val email: State<String> = _email

    private val _emailError = mutableStateOf(savedStateHandle.get("emailError") ?: "")
    val emailError: State<String> = _emailError

    private val _password = mutableStateOf(savedStateHandle.get("password") ?: "")
    val password: State<String> = _password

    private val _passwordError = mutableStateOf(savedStateHandle.get("passwordError") ?: "")
    val passwordError: State<String> = _passwordError

    private val USER_POOL_ADDRESS = "cognito-idp.us-east-1.amazonaws.com/us-east-1_9H0qG2JDz"

    fun login(){
        signIn.invoke(email.value?:"",password.value?:"")
            .flowOn(Dispatchers.IO)
            .onStart { _loading.value = true }
            .onCompletion { _loading.value = false }
            .onEach {
                viewModelScope.launch {
                    _uiEvent.emit(UiEvent.Success)
                    Timber.d("login $it.")
                }
            }
            .catch { e ->
                Timber.e("login failure: $e")
                if (e is UsernameException)
                    if (e is UsernameExistsException)
                        _emailError.value =
                            application.getString(R.string.account_email_has_been_used)
                    else
                        e.message?.let { _emailError.value = it }
                else
                    viewModelScope.launch {
                        _uiEvent.emit(
                            UiEvent.Fail(e.message ?: "signIn error")
                        )
                    }
            }
            .launchIn(viewModelScope)
    }

    fun logOut(){
        signOut.invoke()
            .flowOn(Dispatchers.IO)
            .onEach {
                viewModelScope.launch {
                    Timber.d("logout success.")
                }
            }
            .catch { e ->
                Timber.e("logOut failure: $e")
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
    fun getTime(){
        getIdToken()
            .flatMapConcat {
                getTimeUseCase(it)
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                Timber.d(it.toString()) }
            .catch { e -> Timber.e(e.toString()) }
            .launchIn(viewModelScope)
    }
    fun setAttachPolicy(){
        getIdToken()
            .flatMapConcat {
                attachPolicyUseCase(it)
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                Timber.d(it.toString()) }
            .catch { e -> Timber.e(e.toString()) }
            .launchIn(viewModelScope)
    }

    fun setCredentialsProvider(){
        getIdToken()
            .map{ idToken ->
                val logins: MutableMap<String, String> = HashMap()
                logins.put(USER_POOL_ADDRESS, idToken)
                cachingCredentialsProvider.logins = logins
            }
            .map {
                mqttStatefulConnection.setCredentialsProvider(cachingCredentialsProvider)
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                Timber.d(it.toString()) }
            .catch { e -> Timber.e(e.toString()) }
            .launchIn(viewModelScope)
    }

    suspend fun checkSignedIn(onSuccess:()->Unit, onFailure:()->Unit){
        isSignedIn()
            .flatMapConcat {
                getUserStateDetails()
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.d(e.toString())
                logOut()
                onFailure.invoke()
            }
            .collectLatest {
                Timber.d("Get Name Once: "+it.toString())
                    onSuccess.invoke()
            }
    }

    fun getShareInvitation(){
        getIdToken()
            .flatMapConcat {
                shareInvitationUseCase(it)
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                Log.d("TAG",it.toString()) }
            .catch { e -> Log.e("TAG",e.toString()) }
            .launchIn(viewModelScope)
    }

    sealed class UiEvent {
        object Success : UiEvent()
        data class Fail(val message: String) : UiEvent()
    }
}