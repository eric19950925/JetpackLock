package com.sunion.ikeyconnect.account_management.my_profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.domain.Interface.AuthRepository
import com.sunion.ikeyconnect.domain.usecase.account.ChangeNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val changeName: ChangeNameUseCase
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _username = mutableStateOf("")
    val username: State<String> = _username

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _saveSuccess = mutableStateOf<Boolean?>(null)
    val saveSuccess: State<Boolean?> = _saveSuccess

    fun loadData() {
        viewModelScope.launch (Dispatchers.Main){ //todo note it
            _isLoading.value = true
            try {
                _username.value = authRepository.getName().singleOrNull() ?: ""
                _email.value = authRepository.getEmail().singleOrNull() ?: ""
            } catch (e: Exception) {
//                Timber.e(e)
            }
            _isLoading.value = false
        }
    }

    fun setUsername(name: String) {
        _username.value = name
    }

    fun saveName() {
        changeName(_username.value)
            .onStart { _isLoading.value = true }
            .onCompletion { _isLoading.value = false }
            .onEach { _saveSuccess.value = true }
            .catch { e ->
//                Timber.e(e)
                _saveSuccess.value = false
            }
            .launchIn(viewModelScope)
    }

    fun clearSaveResult() {
        _saveSuccess.value = null
    }
}