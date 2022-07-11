package com.sunion.ikeyconnect.settings.event_log

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.sunion_service.EventGetResponse
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EventLogViewModel @Inject constructor(
    private val iotService: SunionIotService,
    private val getUuid: GetUuidUseCase,
    private val toastHttpException: ToastHttpException,
): ViewModel(){
    private val _uiState = MutableStateFlow(EventLogUiState())
    val uiState: StateFlow<EventLogUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<EventLogUiEvent>()
    val uiEvent: SharedFlow<EventLogUiEvent> = _uiEvent

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val lockEvents = mutableStateListOf<EventGetResponse.LockGeneral.Events>()

    private var _thingName: String? = null
    val thingName: String?
        get() = _thingName

    private var _deviceName: String? = null
    val deviceName: String?
        get() = _deviceName

    fun init(thingName: String, deviceName: String) {
        _thingName = thingName
        _deviceName = deviceName
        getEvent()
        _uiState.update { it.copy(isLoading = true) }
    }

    private fun getEvent(){
        flow { emit( iotService.getEvent(
            thingName ?: throw Exception("Thing Name is Empty."),
            (System.currentTimeMillis()/1000).toInt(),
            getUuid.invoke()
            ) )
        }
            .onEach { response ->
                response.lockGeneral?.events?.forEach { event ->
                    lockEvents.add(event)
                }
                _uiState.update { it.copy(events = lockEvents) }
                _uiState.update { it.copy(isLoading = false) }
            }
            .catch { e ->
//                Timber.e(e)
                toastHttpException(e)
                viewModelScope.launch { _uiEvent.emit(EventLogUiEvent.EventLogFail) }
                _uiState.update { it.copy(isLoading = false) }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)


    }
}
data class EventLogUiState(
    val showDeleteConfirmDialog: Boolean = false,
    val events: MutableList<EventGetResponse.LockGeneral.Events> = mutableStateListOf(),
    val isLoading: Boolean = false,
)

sealed class EventLogUiEvent {
    object EventLogFail : EventLogUiEvent()
}