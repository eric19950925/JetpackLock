package com.sunion.ikeyconnect.settings.event_log

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.sunion_service.EventGetResponse
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.home.HomeViewModel
import com.sunion.ikeyconnect.lock.AllLock
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EventLogViewModel @Inject constructor(
    private val iotService: SunionIotService,
    private val getUuid: GetUuidUseCase,
    private val toastHttpException: ToastHttpException,
    private val lockProvider: LockProvider,
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


    fun init(thingName: String, deviceName: String, deviceType: Int) {
        _thingName = thingName
        _deviceName = deviceName
        if(deviceType == HomeViewModel.DeviceType.WiFi.typeNum) getEvent() else getEventByBle(thingName)
        _uiState.update { it.copy(isLoading = true) }
    }

    private fun getEventByBle(deviceIdentity: String) {
        flow { emit(lockProvider.getLockByMacAddress(deviceIdentity)) }
            .map { lock ->
                (lock as AllLock).getTimeZone()
                lock to (lock as AllLock).getEventQuantity()
            }
            .map { (lock, quantity) ->
                val amountOfLog = if(quantity > 150)150 else quantity
                Observable
                    .fromIterable((0..amountOfLog.minus(1)).reversed())
                    .asFlow()
                    .flatMapConcat { index -> (lock as AllLock).getEventByBle(index) }
                    .toList()
                    .sortedBy { it.eventTimeStamp }
                    .reversed()
            }
            .map {
                it.forEach { eventlog ->
                    lockEvents.add(
                        EventGetResponse.LockGeneral.Events(
                        Type = eventlog.event.toString(),
                        Millisecond = eventlog.eventTimeStamp * 1000,
                        extraDetail = EventGetResponse.LockGeneral.Events.ExtraDetail(
                            Actor = eventlog.name,
                            Message = "..."
                        )
                    ))
                }
                _uiState.update { state -> state.copy(events = lockEvents, isLoading = false) }
            }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
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