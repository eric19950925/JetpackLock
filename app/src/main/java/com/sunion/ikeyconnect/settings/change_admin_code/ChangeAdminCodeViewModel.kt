package com.sunion.ikeyconnect.settings.change_admin_code

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.usecase.account.*
import com.sunion.ikeyconnect.domain.usecase.device.VerifyAccessCodeStrengthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChangeAdminCodeViewModel @Inject constructor(
    private val application: Application,
    private val verifyAccessCodeStrength: VerifyAccessCodeStrengthUseCase,
    private val iotService: SunionIotService,
    private val getUuid: GetUuidUseCase,
    private val toastHttpException: ToastHttpException,
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

    private val _saveSuccess = mutableStateOf<Boolean?>(null)
    val saveSuccess: State<Boolean?> = _saveSuccess

    private val errorFieldIsRequired =
        application.getString(R.string.account_this_field_is_required)

    private var _deviceIdentity: String? = null
    val deviceIdentity: String?
        get() = _deviceIdentity

    fun init(DeviceIdentity: String) {
        _deviceIdentity = DeviceIdentity
    }


    fun setCurrentPassword(password: String) {
        _currentPassword.value = password

        if (currentPassword.value.isBlank())
            _currentPasswordError.value =
                "Can not be empty."

        if (!verifyAccessCodeStrength(password))
            _currentPasswordError.value = "4-8"
        else _currentPasswordError.value = ""
    }

    fun setNewPassword(password: String) {

        _newPassword.value = password

        if (!verifyAccessCodeStrength(password))
            _newPasswordError.value = "4-8"
        else _newPasswordError.value = ""
    }

    fun setConfirmNewPassword(password: String) {
        _confirmNewPasswordError.value = ""

        _confirmNewPassword.value = password

        if (newPassword.value != password)
            _confirmNewPasswordError.value =
                application.getString(R.string.account_password_does_not_match)
    }

    fun onSaveClick() {
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

        flow { emit(iotService.getAdminCode(
            thingName = deviceIdentity?:throw Exception("deviceIdentity is null"),
            clientToken = getUuid.invoke(),
        )) }
            .map { accessCodeGetResponse ->
                Timber.d(accessCodeGetResponse.accessCode.toString())
                if(accessCodeGetResponse.accessCode.isEmpty())throw Exception("You had not set admin code.")
                (accessCodeGetResponse.accessCode[0]?.code ?: "") == currentPassword.value
            }.map {
                if(it)changeAdminCode()
                else throw Exception("Old Admin Code Error.")
            }
            .flowOn(Dispatchers.IO)
            .onStart { _isLoading.value = true }
            .catch { e ->
                toastHttpException(e)
            }
            .launchIn(viewModelScope)

    }

    fun changeAdminCode() {
        flow { emit(iotService.updateAdminCode(
            thingName = deviceIdentity?:throw Exception("deviceIdentity is null"),
            clientToken = getUuid.invoke(),
            adminCode = newPassword.value,
            oldCode = currentPassword.value,
        )) }
            .flowOn(Dispatchers.IO)
            .onCompletion { _isLoading.value = false }
            .onEach { viewModelScope.launch { _saveSuccess.value = true } }
            .catch { e ->
                toastHttpException(e)
                viewModelScope.launch { _saveSuccess.value = false }
            }
            .launchIn(viewModelScope)
    }

    fun clearSaveResult() {
        _saveSuccess.value = null
    }
}