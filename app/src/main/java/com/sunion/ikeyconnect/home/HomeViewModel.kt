package com.sunion.ikeyconnect.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttSubscriptionStatusCallback
import com.google.gson.Gson
import com.sunion.ikeyconnect.*
import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.api.APIObject
import com.sunion.ikeyconnect.data.PreferenceStore
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetIdentityIdUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.domain.usecase.device.CollectWIfiStateUseCase
import com.sunion.ikeyconnect.domain.usecase.device.IsNetworkConnectedUseCase
import com.sunion.ikeyconnect.domain.usecase.home.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.json.JSONTokener
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferenceStore: PreferenceStore,
    private val mqttStatefulConnection: MqttStatefulConnection,
    private val userSync: UserSyncUseCase,
    private val getDeviceList: GetDeviceListUseCase,
    private val getIdToken: GetIdTokenUseCase,
    private val getIdentityId: GetIdentityIdUseCase,
    private val iotService: SunionIotService,
    private val isNetworkConnectedUseCase: IsNetworkConnectedUseCase,
    private val collectWIfiState: CollectWIfiStateUseCase,
    private val getUuid: GetUuidUseCase,
    private val provisionDomain: ProvisionDomain,
    private val toastHttpException: ToastHttpException,
    ) :
    ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent

    private val wifiLocksList = mutableStateListOf<WiFiLock>()
    private val bleLocksList = mutableStateListOf<BleLock>()
    private var tempOrderList = mutableListOf<UserSyncOrder>()

    private var isListHasDiff = false
    private var currentIndex = 0
    private var previousDeadbolt = LockStatus.LOCKED

    private var homePageListScope = CoroutineScope(Dispatchers.IO)

    private val changeNameJobs: MutableMap<String, Job> = mutableMapOf()
    private val updateTimes: MutableMap<String, LocalDateTime> = mutableMapOf()
    private val hasBeenCollectedBleConnectStatus: MutableList<String> = mutableListOf()

    val connectionState: SharedFlow<ConnectMqttUiEvent> = mqttStatefulConnection.connectionState

    private val _showGuide = mutableStateOf(false)
    val showGuide: State<Boolean> = _showGuide

    private val _wifiLocksFromMqtt = MutableSharedFlow<MutableList<WiFiLock>?>()
    private val wifiLocksFromMqtt: SharedFlow<MutableList<WiFiLock>?> = _wifiLocksFromMqtt
    override fun onCleared() {
        super.onCleared()
        Timber.tag("HomeViewModel").d("onCleared")
    }

    init {
        _uiState.update { it.copy(showGuide = !preferenceStore.isGuidePopupMenuPressed) }

        collectMqttConnectionState()

        // network status
        _uiState.update { it.copy(networkAvailable = isNetworkConnectedUseCase()) }
        collectWIfiState()
            .flowOn(Dispatchers.IO)
            .onEach { available -> _uiState.update { it.copy(networkAvailable = available) } }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

//        getAllLocksInfoUseCase.invoke()
//            .map {
//                it.forEach {
//                    Timber.d(it.macAddress + " " + it.keyTwo)
//                }
//            }.subscribeOn(Schedulers.io()).subscribe()
    }

    private fun collectMqttConnectionState() {
        connectionState
            .onEach { event ->
                Timber.d(event.toString())
                when(event){
                    ConnectMqttUiEvent.Prepared -> {
                        mqttStatefulConnection.connectMqtt()
                        _uiState.update { it.copy(isLoading = true, loadingMessage = "Mqtt Connecting...") }
                        homePageListScope.launch{
                            delay(60000)
                            _uiState.update { it.copy(isLoading = false) }
                            prepareDataToDraw()
                            homePageListScope.cancel()
                        }
                    }
                    ConnectMqttUiEvent.Connected -> {
                        mqttStatefulConnection.unsubscribeAllTopic()
                        getDeviceList.unsubscribeAllTopic()
                        prepareDataToDraw()
                    }
                    ConnectMqttUiEvent.ConnectionLost -> disconnectWiFiLock()
                    ConnectMqttUiEvent.Reconnecting -> disconnectWiFiLock()
                    else -> {
                        Timber.d(event.toString())
                    }
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private suspend fun prepareDataToDraw() {
        val idToken = runCatching {
            getIdToken().single()
        }.getOrNull() ?: return

        getIdentityId()
            .map { identityId ->
                mqttStatefulConnection.subscribeApiPortal(
                    identityId,
                    callbackMqttApiPortal(),
                    callbackForPub(idToken, identityId, getUuid.invoke()))
            }
            .launchIn(viewModelScope)
    }
    private fun callbackForPub(idToken: String, identityId: String, getUuid: String) = object:
        AWSIotMqttSubscriptionStatusCallback {
        override fun onSuccess() {
            Timber.d("===// Sub success do Pub //===")
            getDeviceList.pubDeviceList(idToken, identityId, getUuid)
            userSync.pubGetUserSync(idToken, identityId, getUuid)
//            userSync.pubUpdateUserSyncExample(idToken, identityId, getUuid, "", "")
        }

        override fun onFailure(exception: Throwable?) { Timber.e(exception) }
    }

    private fun callbackMqttApiPortal() = AWSIotMqttNewMessageCallback { _: String?, data: ByteArray? ->
        try {
            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            Timber.d("callbackForMqttDeviceList: $message")
            val jsonObject = JSONTokener(message).nextValue() as JSONObject
            val apiName = jsonObject.optNullableString("API")?:throw IllegalArgumentException("Unknown API Mqtt Callback Msg.")
            val responseBody = jsonObject.getJSONObject("ResponseBody")
            val payloadString = responseBody.optNullableString("Payload")
            val responseBodyString = jsonObject.getString("ResponseBody")

            when(apiName){
                APIObject.DeviceList.route -> {
                    val resBody = Gson().fromJson(responseBodyString, DeviceListResponseBody::class.java)
                    val deviceList = resBody.Devices?:return@AWSIotMqttNewMessageCallback
                    wifiLocksUpdate(deviceList)
                }
                APIObject.GetUserSync.route -> {
                    //prepare order list for drawing

                    val payload = Gson().fromJson(payloadString, ResponsePayload::class.java)
                    payload.Dataset.BLEDevices?.Devices?.let { bleLocksList.addAll( it ) }
                    val orderList = payload.Dataset.DeviceOrder?.Order

                    Timber.d(orderList.toString())
                    orderListUpdate(orderList?: emptyList())

                    /**Cloud will response one list one time.*/
                    val version = responseBody.getJSONObject("Payload").getJSONObject("Dataset").optNullableJSONObject("DeviceOrder")?.getInt("version")
                    userSync.updateModifyVersion(version?:0)

                }
                APIObject.UpdateUserSync.route -> {
//                    val resBody = Gson().fromJson(responseBodyString, UserSyncResponseBody::class.java)
//                    val BleLockList = resBody.Filter.BleLockInfo
//                    val orderList = resBody.Filter.BleLockInfo?.Data
//                    orderListUpdate(orderList?:return@AWSIotMqttNewMessageCallback)

                    Timber.d(message)
                }
                APIObject.DeviceProvision.route -> {
                    val thingName = responseBody.optNullableString("ThingName")?:throw IllegalArgumentException("ThingName is Null.")
                    provisionDomain.provisionThingName = thingName
                    Timber.d("Now you can set provision of thing-$thingName.")
                }
            }

        } catch (e: Exception) {
            Timber.d("Message encoding error.", e)
        }
    }

    private fun orderListUpdate(orderList: List<UserSyncOrder>) {
        val myOrderList = mutableListOf<UserSyncOrder>()
        orderList.forEach {
            myOrderList.add(it)
        }

        _uiState.update { it.copy(lockOrder = myOrderList, isLoading = false) }
        createListForView()
//        val typeToken = object : TypeToken<OrderData>() {}.type
//        val new_OrderList = Gson().fromJson<ArrayList<OrderData>>(orderList, typeToken)
//        Timber.d(new_OrderList.toString())

        //Create user sync list then subpub each wifi lock
//        collectOrderListFromMqtt()
//        runBlocking {
//            //store the device into state lock list
//            updateOrderList(deviceList)
//        }
    }

    private fun wifiLocksUpdate(deviceList: List<DeviceThing>) {
        collectWifiLocksFromMqtt()
        runBlocking {
            //store the device into state lock list
            updateWifiLocks(deviceList)
        }
    }

    private fun collectWifiLocksFromMqtt(){
        wifiLocksFromMqtt
            .onEach { locks ->
                wifiLocksList.clear()
                wifiLocksList.addAll(locks ?: throw Exception("device is null"))
                Timber.d("Clean and Update the list.")
            }
            .take(1)//test
            .flatMapConcat {
                flowForGetThingShadow(it?: throw IllegalArgumentException("Device list is empty.") )
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun flowForGetThingShadow(list: List<WiFiLock>) = flow { emit (
        list.forEach {
            subPubGetThingShadow(it.ThingName)
            subPubUpdateThingShadow(it.ThingName)
        }
    )}

    fun setCurrentPage(page: Int) {
        Timber.d("page:$page")
        _uiState.update { it.copy(currentPage = page) }
        currentIndex = page
    }

    private fun subPubGetThingShadow(thingName: String) =
        flow {
                emit(getDeviceList.subGetThingShadow(thingName, getUuid.invoke(), callbackMqttGetThingShadow(thingName)))
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

    private fun callbackMqttGetThingShadow(thingName: String) = AWSIotMqttNewMessageCallback { _: String?, data: ByteArray? ->
        try {

            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            Timber.d("callbackMqttGetThing Msg: $message")
            val jsonObject = JSONTokener(message).nextValue() as JSONObject
            val lockState = jsonObject.getJSONObject("state")
            val callback_clientToken = jsonObject.optNullableString("clientToken")

            val reported = lockState.optNullableString("reported")?:throw Exception("ThingShadow's reported is Null.")

            val shadowReported = Gson().fromJson(reported, Reported::class.java)
            if(callback_clientToken != getUuid.invoke() && callback_clientToken != "")throw Exception("Different Client Token.")

            Timber.d("callbackMqttGetThing: $shadowReported")
            runCatching {
                Timber.d("Lock Deadbolt: ${shadowReported.Deadbolt}, Lock Connected: ${shadowReported.Connected}")
                updateWifiLockListByMqtt(thingName, shadowReported)
            }.onFailure { Timber.e(it) }
        } catch (e: Exception) {
            Timber.d("Message encoding error.", e)
        }
    }


    private fun subPubUpdateThingShadow(thingName: String) =
        getIdToken()
            .map { idToken ->
                getDeviceList.subUpdateThingShadow(thingName, callbackMqttUpdateThingShadow(thingName))
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

    private fun callbackMqttUpdateThingShadow(thingName: String) = AWSIotMqttNewMessageCallback { _: String?, data: ByteArray? ->
        try {
            homePageListScope.cancel()

            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            Timber.d("UpdateThingcallback: $message")
            val jsonObject = JSONTokener(message).nextValue() as JSONObject
            val current = jsonObject.getJSONObject("current")
            val callback_clientToken = jsonObject.optNullableString("clientToken")?:"device"
            Timber.d("callback_clientToken: $callback_clientToken")

            val lockState = current.getJSONObject("state")
            val reported = lockState.getString("reported")
            val shadowReported = Gson().fromJson(reported, Reported::class.java)
            Timber.d(shadowReported.toString())

            if(callback_clientToken != getUuid.invoke() && callback_clientToken != "device")throw Exception("Different Client Token.")
            runBlocking {
                Timber.d("Thing-${thingName.substring(0,3)} callbackMqttUpadteThing: $shadowReported")
                Timber.d("Lock Deadbolt: ${shadowReported.Deadbolt}, Lock Connected: ${shadowReported.Connected}")
                updateWifiLockListByMqtt(thingName, shadowReported)
            }
        } catch (e: Exception) {
            Timber.d("Message encoding error.", e)
        }
    }

    private fun updateWifiLockListByMqtt(deviceIdentity: String, lockState: Reported){
        wifiLocksList.indexOfFirst { it.ThingName == deviceIdentity }.let { index ->
            wifiLocksList[index] = wifiLocksList[index].copy( LockState = lockState )

            //todo???
            _uiState.update { it.copy(isLoading = false) }
            if(wifiLocksList[index].LockState.Deadbolt.getLockState() != previousDeadbolt || !wifiLocksList[index].LockState.Connected){
                val loadingList = uiState.value.loadingLocks
                loadingList.removeIf { it.DeviceIdentity == deviceIdentity }
                _uiState.update { it.copy(loadingLocks = loadingList) }
            }
        }
        if(wifiLocksList.all { it.LockState.Deadbolt != "" }) createListForView()
    }
    private fun createListForView(){
        val newList = mutableListOf<SunionLock>()

        val orderList = uiState.value.lockOrder
//        orderList.sortBy { it.Order }

        orderList.forEach { order ->
            newList.add(
                when(mapDeviceType(order.DeviceType)){
                    DeviceType.WiFi.typeNum -> {
                        val wiFiLock = wifiLocksList.find{ it.ThingName == order.DeviceIdentity }?:throw Exception("WiFiLock's info is null.")
                        SunionLock(DeviceIdentity = order.DeviceIdentity, wiFiLock.Attributes, wiFiLock.LockState, null, DeviceType.WiFi.typeNum, order.Order)
                    }
                    DeviceType.Ble.typeNum -> {
                        val bleLock = bleLocksList.find { it.MACAddress == order.DeviceIdentity }?:throw Exception("BleLock's info is null.")
                        SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.Ble.typeNum, order.Order)
                    }
                    DeviceType.BleMode.typeNum -> {
                        val bleLock = bleLocksList.find { it.MACAddress == order.DeviceIdentity }?:throw Exception("BleLock's info is null.")
                        SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.BleMode.typeNum , order.Order)
                    }
                    else -> { return@forEach }
                }
            )
        }
        //update ui state
//        newList.sortBy { it.Order }
        _uiState.update { it.copy(locks = newList) }
    }

    sealed class DeviceType(val typeNum: Int) {
        object WiFi : DeviceType(1)
        object Ble : DeviceType(2)
        object BleMode : DeviceType(3)
    }
    private suspend fun updateWifiLocks(devices: List<DeviceThing>) {
        val wifiLockList = mutableListOf<WiFiLock>()
        devices.forEach { deviceThing ->
            val wifilock = WiFiLock(
                ThingName = deviceThing.ThingName,
                Attributes = deviceThing.Attributes,
                LockState = Reported(0,0,0,0,0,"","unknown",0,false)
            )
            wifiLockList.add(wifilock)
        }
        Timber.d("Device list is prepared: $wifiLockList")
        _wifiLocksFromMqtt.emit(wifiLockList)
    }

    fun onDrawer(isOpen: Boolean){
        Timber.d("Drawer is Open = $isOpen")
        if(!isOpen && isListHasDiff){
            Timber.d("Update order list.")
            updateOrderList()
        }
        isListHasDiff = false //每次開關都要重製，以免收合時以為有改動過
    }

    private fun updateOrderList(){
        flow { emit(getIdToken().single()) }
            .map { idToken ->
                val identityId = getIdentityId().single()
                (idToken to identityId)
            }
            .map { (idToken, identityId) ->
                userSync.updateOrderList(idToken, identityId, getUuid.invoke(), tempOrderList)
            }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true, loadingMessage = "List ReOrder.") } }
            .onCompletion {
//                delay(1000)
//                _uiState.update { it.copy(isLoading = false) }
            }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }

    fun isConnected(macAddress: String): Boolean =
        uiState.value.locks.find { it.DeviceIdentity == macAddress }?.LockState?.Connected ?: false
//        uiState.value.locks.find { it.Attributes.Bluetooth.MACAddress == macAddress }?.LockState?.Connected ?: false

    fun getBattery(macAddress: String): Int =
        uiState.value.locks.find { it.DeviceIdentity == macAddress }?.LockState?.Battery ?: 0

    fun onLockClick() {
        val lock = uiState.value.locks[currentIndex]
        previousDeadbolt = lock.LockState?.Deadbolt?.getLockState()?:return

        if(lock.LockType != DeviceType.WiFi.typeNum) {
            if(uiState.value.loadingLocks.contains(lock))return
            //ble connect
        }else {
            if(!lock.LockState.Connected || uiState.value.loadingLocks.contains(lock))return

            val loadingList = uiState.value.loadingLocks
            loadingList.add(lock)
            _uiState.update { it.copy(loadingLocks = loadingList) }
            clickWiFiLock(lock)
        }
    }

    private fun clickWiFiLock(lock: SunionLock) {
        if(lock.LockState?.Direction == "unknown"){
            flow { emit(iotService.checkOrientation(lock.DeviceIdentity, getUuid.invoke())) }
                .catch { e -> toastHttpException(e) }
                .flowOn(Dispatchers.IO)
                .launchIn(viewModelScope)
        }else {
            if (lock.LockState?.Deadbolt == "unlock") {
                flow { emit(iotService.lock(lock.DeviceIdentity, getUuid.invoke())) }
                    .catch { e -> toastHttpException(e) }
                    .flowOn(Dispatchers.IO)
                    .launchIn(viewModelScope)
            }
            else {
                flow { emit(iotService.unlock(lock.DeviceIdentity, getUuid.invoke())) }
                    .catch { e -> toastHttpException(e) }
                    .flowOn(Dispatchers.IO)
                    .launchIn(viewModelScope)
            }
        }
    }

    /**
     * Change home page locks' UI to disconnection.
     */
    private fun disconnectWiFiLock() = runBlocking {
        _uiState.update { it.copy(networkAvailable = isNetworkConnectedUseCase()) }
        val newList = mutableListOf<SunionLock>()

        val orderList = uiState.value.lockOrder
//        orderList.sortBy { it.Order }

        orderList.forEach { order ->
            newList.add(
                when(mapDeviceType(order.DeviceType)){
                    DeviceType.WiFi.typeNum -> {
                        val wiFiLock = wifiLocksList.find{ it.ThingName == order.DeviceIdentity }?:throw Exception("WiFiLock's info is null.")
                        SunionLock(DeviceIdentity = order.DeviceIdentity, wiFiLock.Attributes,
                            LockState = Reported(
                                Battery = wiFiLock.LockState.Battery,
                                Rssi = wiFiLock.LockState.Rssi,
                                Status = wiFiLock.LockState.Status,
                                RegistryVersion = wiFiLock.LockState.RegistryVersion,
                                AccessCodeTime = wiFiLock.LockState.AccessCodeTime,
                                Deadbolt = "disconnect",
                                Searchable = wiFiLock.LockState.Searchable,
                                Direction = wiFiLock.LockState.Direction,
                                Connected = false,
                            ),
                            null, DeviceType.WiFi.typeNum, order.Order)
                    }
                    DeviceType.Ble.typeNum -> {
                        val bleLock = bleLocksList.find { it.MACAddress == order.DeviceIdentity }?:throw Exception("BleLock's info is null.")
                        SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.Ble.typeNum, order.Order)
                    }
                    DeviceType.BleMode.typeNum -> {
                        val bleLock = bleLocksList.find { it.MACAddress == order.DeviceIdentity }?:throw Exception("BleLock's info is null.")
                        SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.BleMode.typeNum , order.Order)
                    }
                    else -> { return@forEach }
                }
            )
        }
        //update ui state
//        newList.sortBy { it.Order }
        _uiState.update { it.copy(locks = newList) }

        val loadingList = uiState.value.loadingLocks
        loadingList.removeIf { it.LockType == DeviceType.WiFi.typeNum }
        _uiState.update { it.copy(loadingLocks = loadingList) }

    }

    fun getUpdateTime(thingName: String) = 0
    fun setLockName(thingName: String, name: String){}
    fun saveName(thingName: String){}

    @OptIn(FlowPreview::class)
    fun boltOrientation(macAddress: String) {
//        loadLocksFlow()
//            .flatMapConcat { flow { emit(lockProvider.getLockByMacAddress(macAddress)) } }
//            .flatMapConcat { flow { emit(it!!.getBoltOrientation(getClientTokenUseCase())) } }
//            .flowOn(Dispatchers.IO)
//            .onStart {
//                Timber.d(lockDevices.toString())
//                lockDevices
//                    .indexOfFirst { it.macAddress == macAddress }
//                    .takeIf { it >= 0 }
//                    ?.let { targetIndex ->
//                        val lock = lockDevices[targetIndex]
//                        if (targetIndex >= 0 && lock is ConnectedDevice)
//                            setLockIsProcessing(targetIndex, true)
//                    }
//            }
//            .onEach { boltOrientation ->
//                Timber.d("boltOrientation:$boltOrientation")
//                viewModelScope.launch {
//                    _uiEvent.emit(
//                        if (boltOrientation != LockOrientation.NotDetermined) HomeUiEvent.BoltOrientationSuccess
//                        else HomeUiEvent.BoltOrientationFailed
//                    )
//                }
//                lockDevices
//                    .indexOfFirst { it.macAddress == macAddress }
//                    .takeIf { it >= 0 }
//                    ?.let { targetIndex ->
//                        val lock = lockDevices[targetIndex]
//                        if (targetIndex >= 0 && boltOrientation == LockOrientation.NotDetermined)
//                            _uiState.update {
//                                it.copy(
//                                    locks = lockDevices.toMutableList().apply {
//                                        set(
//                                            targetIndex,
//                                            BoltOrientationFailDevice(
//                                                macAddress = lock.macAddress,
//                                                connectionState = lock.connectionState,
//                                                permission = lock.permission,
//                                                name = lock.name,
//                                                createdAt = lock.createdAt,
//                                                lockOrientation = lock.lockOrientation,
//                                                isLocked = LockStatus.UNLOCKED,//TODO
//                                                displayIndex = lock.displayIndex
//                                            )
//                                        )
//                                    }
//                                )
//                            }
//                    }
//            }
//            .catch { Timber.e(it) }
//            .launchIn(viewModelScope)
    }

    private fun getDeviceListByNetwork() {
        getIdToken()
            .map { idToken ->
                iotService.getDeviceList("clientToken")
            }
            .map {
                it.forEach {
                    Timber.d(it.toString())
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    fun setGuideHasBeenSeen() {
        preferenceStore.isGuidePopupMenuPressed = true
        _uiState.update { it.copy(showGuide = false) }
    }

    fun changeOrderList(list: MutableList<UserSyncOrder>) {
        //if list has been reorder, then wait for update(close side bar)
        isListHasDiff = list != uiState.value.lockOrder
        tempOrderList = list
    }

    fun getDeviceType(deviceIdentity: String) =
        when(uiState.value.locks.find { it.DeviceIdentity == deviceIdentity }?.LockType){
            1 -> "wifi"
            2 -> "ble"
            3 -> "ble mode"
            else -> ""
        }

}

data class HomeUiState(
    val showGuide: Boolean = false,
    val isLoading: Boolean = false,
    val loadingLocks: MutableList<SunionLock> = mutableStateListOf(),
    val loadingMessage: String = "loading ...",
    val locks: MutableList<SunionLock> = mutableStateListOf(),
    var lockOrder: MutableList<UserSyncOrder> = mutableStateListOf(),
    val currentPage: Int = 0,
    val networkAvailable: Boolean = false,
)

sealed class HomeUiEvent {
    object BoltOrientationSuccess : HomeUiEvent()
    object BoltOrientationFailed : HomeUiEvent()
}