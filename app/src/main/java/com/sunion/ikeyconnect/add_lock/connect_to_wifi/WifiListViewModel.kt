package com.sunion.ikeyconnect.add_lock.connect_to_wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.domain.Interface.WifiListResult
import com.sunion.ikeyconnect.domain.blelock.BluetoothConnectState
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import com.sunion.ikeyconnect.lock.WifiLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WifiListViewModel @Inject constructor(
    private val getClientTokenUseCase: GetClientTokenUseCase,
    private val statefulConnection: ReactiveStatefulConnection,
    private val lockProvider: LockProvider,
    ) :
    ViewModel() {
    private val _uiState = MutableStateFlow(WiFiListUiState())
    val uiState: StateFlow<WiFiListUiState> = _uiState

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress

    private var lock: WifiLock? = null
    private var isLockDisconnected = false
    private var scanWifiJob: Job? = null

    fun init(macAddress: String) {
        _uiState.value = WiFiListUiState()
        this._macAddress = macAddress

        flow { emit(lockProvider.getLockByMacAddress(macAddress) as WifiLock?) }
            .flowOn(Dispatchers.IO)
            .onEach {
                lock = it
                it?.let {
                    collectBleConnectionState()
                    collectWifiList()
                    if (it.isConnected()) {
                        scanWIfi()
                    } else {
                        isLockDisconnected = true
                        it.connect()
                    }
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun collectWifiList() {
        val wifiLock = lock ?: return
        wifiLock
            .collectWifiList()
            .flowOn(Dispatchers.IO)
            .onEach { wifi ->
                when (wifi) {
                    WifiListResult.End -> _uiState.update { it.copy(isScanning = false) }
                    is WifiListResult.Wifi -> {
                        val wifiInfo = WifiInfo(wifi.ssid, wifi.needPassword, 0)
                        _uiState.update {
                            it.copy(wifiList = uiState.value.wifiList.toMutableList()
                                .apply { add(wifiInfo) })
                        }
                    }
                }
            }
            .catch { Timber.e("set noti error: $it") }
            .launchIn(viewModelScope)
    }

    private fun collectBleConnectionState() {
        val wifiLock = lock ?: return
        wifiLock
            .connectionState2
            .onEach { state ->
                Timber.d(state.toString())
                if (state == BluetoothConnectState.CONNECTED && isLockDisconnected)
                    scanWIfi()
//                else if (state == BluetoothConnectState.DISCONNECTED)
//                    reconnect()
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun reconnect() {
        val wifiLock = lock ?: return
        isLockDisconnected = true
        runCatching { wifiLock.connect() }.getOrElse { Timber.e(it) }

        flow { emit(delay(30000)) }
            .flowOn(Dispatchers.IO)
            .onEach {
                if (!wifiLock.isConnected()) {
                    _uiState.update { it.copy(showDisconnect = true) }
                    wifiLock.disconnect()
//                    wifiLock.delete(getClientTokenUseCase())
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    fun scanWIfi() {
        val wifiLock = lock ?: return
        if (scanWifiJob != null) return // if is scaning, won't repeat to scan wifi.
        _uiState.update { it.copy(isScanning = true) }
        scanWifiJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching { wifiLock.scanWifi() }.getOrElse { Timber.e(it) }
            scanWifiJob = null
        }
    }
}

data class WiFiListUiState(
    val wifiList: List<WifiInfo> = emptyList(),
    val isScanning: Boolean = false,
    val showDisconnect: Boolean = false
)