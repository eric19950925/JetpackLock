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
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetIdentityIdUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.domain.usecase.device.CollectWIfiStateUseCase
import com.sunion.ikeyconnect.domain.usecase.device.GetAllLocksInfoUseCase
import com.sunion.ikeyconnect.domain.usecase.device.IsNetworkConnectedUseCase
import com.sunion.ikeyconnect.domain.usecase.home.*
import com.sunion.ikeyconnect.lock.AllLock
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
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
    private val lockInformationRepository: LockInformationRepository,
    private val lockProvider: LockProvider,
    private val statefulConnection: ReactiveStatefulConnection,
    private val getAllLocksInfoUseCase: GetAllLocksInfoUseCase
    ) :
    ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent

    private val wifiLocksList = mutableListOf<WiFiLock>()
    private val bleLocksList = mutableListOf<BleLock>()
    private var deviceOrderList = mutableListOf<UserSyncOrder>()
    private var tempOrderList = mutableListOf<UserSyncOrder>()

    private var isListHasDiff = false
    private var currentIndex = 0
    private var previousDeadbolt = LockStatus.LOCKED

    private var homePageListScope = CoroutineScope(Dispatchers.IO)

    private val changeNameJobs: MutableMap<String, Job> = mutableMapOf()
    private val updateTimes: MutableMap<String, LocalDateTime> = mutableMapOf()
    private val hasBeenCollectedBleConnectStatus: MutableList<String> = mutableListOf()

    private val mqttConnectionState: SharedFlow<ConnectMqttUiEvent> = mqttStatefulConnection.connectionState

    private val bleConnectionState: SharedFlow<Event<Pair<Boolean, String>>> = statefulConnection.connectionState

    private val bleLockState: SharedFlow<LockSetting> = statefulConnection.bleLockState

    private val _showGuide = mutableStateOf(false)
    val showGuide: State<Boolean> = _showGuide

    var currentBleLock: AllLock ?= null
    var currentBleLockState = Reported(
        Direction = "orientation",
        Deadbolt = "deadbolt",
        Connected = true,
        Battery = 0,
        Rssi = 0,
        RegistryVersion = 0,
        AccessCodeTime = 0,
        Searchable = 0,
        Status = 0,
    )

    override fun onCleared() {
        super.onCleared()
        currentBleLock?.disconnect()

        Timber.tag("HomeViewModel").d("onCleared")
    }

    init {
        _uiState.update { it.copy(showGuide = !preferenceStore.isGuidePopupMenuPressed) }

        collectMqttConnectionState()
        collectBleLockStateToShow()

        // network status
        _uiState.update { it.copy(networkAvailable = isNetworkConnectedUseCase()) }
        collectWIfiState()
            .flowOn(Dispatchers.IO)
            .onEach { available -> _uiState.update { it.copy(networkAvailable = available) } }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

        getAllLocksInfoUseCase.invoke()
            .map {
                Timber.d("LocksList in DB'size = ${it.size}")
                it.forEach {
                    Timber.d(it.toString())
                }
            }.subscribeOn(Schedulers.io()).subscribe()
    }

    private fun collectMqttConnectionState() {
        mqttConnectionState
            .onEach { event ->
                Timber.d(event.toString())
                when(event){
                    ConnectMqttUiEvent.Prepared -> {
                        mqttStatefulConnection.connectMqtt()
                        _uiState.update { it.copy(isLoading = true, loadingMessage = "Mqtt Connecting...") }
                        homePageListScope.launch{
                            delay(60000)
                            _uiState.update { it.copy(isLoading = false) }
                            subscribeApiPortal()
                            homePageListScope.cancel()
                        }
                    }
                    ConnectMqttUiEvent.Connected -> {
                        mqttStatefulConnection.unsubscribeAllTopic()
                        getDeviceList.unsubscribeAllTopic()
                        subscribeApiPortal()
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

    private suspend fun subscribeApiPortal() {
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
            Timber.d("===// Subscribe ApiPortal: getDeviceList and UserSync-Order //===")
            getDeviceList.pubDeviceList(idToken, identityId, getUuid)
            _uiState.update { it.copy(loadingMessage = "DeviceList...") }
//            cleanUserSync()
        }

        override fun onFailure(exception: Throwable?) { Timber.e(exception) }
    }
    private fun cleanUserSync(){
        flow { emit(userSync.getUserSync(getUuid.invoke())) }
            .map {
                val orderData = it.Payload.Dataset.DeviceOrder
                val bleLockData = it.Payload.Dataset.BLEDevices
                (orderData to bleLockData)
            }
            .map { (orderData, bleLockData) ->
                userSync.updateUserSync(getUuid.invoke(), UserSyncRequestPayload(
                    Dataset = RequestDataset(
                        DeviceOrder = RequestOrder(null, orderData?.version?:0),
                        BLEDevices = RequestDevices(null, bleLockData?.version?:0)
                    )
                )
                )
            }
            .flowOn(Dispatchers.IO)
            .onCompletion {
                _uiState.update { it.copy(isLoading = false) }
            }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }

    private fun updateUserSyncWithBleName(name: String){
        flow { emit(userSync.getUserSync(getUuid.invoke())) }
            .map {
                val orderData = it.Payload.Dataset.DeviceOrder
                val bleLockData = it.Payload.Dataset.BLEDevices
                userSync.updateOrderModifyVersion(orderData?.version?:0)
                (orderData to bleLockData)
            }
            .map { (orderData, bleLockData) ->
                val oldBleDev = bleLockData?.Devices
                val newBleDev = mutableListOf<BleLock>()
                oldBleDev?.forEach { oldLock ->
                    newBleDev.add(
                        if (oldLock.MACAddress == currentBleLock?.lockInfo?.macAddress) {
                            BleLock(
                                MACAddress = oldLock.MACAddress,
                                DisplayName = name,
                                ConnectionKey = oldLock.ConnectionKey,
                                OneTimeToken = oldLock.OneTimeToken,
                                PermanentToken = oldLock.PermanentToken,
                                SharedFrom = oldLock.SharedFrom
                            )
                        } else oldLock
                    )
                }
                (newBleDev to bleLockData?.version)
            }
            .map { (newBleDev, version) ->
                userSync.updateUserSync(getUuid.invoke(), UserSyncRequestPayload(
                    Dataset = RequestDataset(
                        DeviceOrder = null,
                        BLEDevices = RequestDevices(newBleDev, version?:0)
                    )
                ))
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }

    private fun callbackMqttApiPortal() = AWSIotMqttNewMessageCallback { _: String?, data: ByteArray? ->
        try {
            val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
            Timber.d("callbackForMqttApiPortal: $message")
            val jsonObject = JSONTokener(message).nextValue() as JSONObject
            val apiName = jsonObject.optNullableString("API")?:throw IllegalArgumentException("Unknown API Mqtt Callback Msg.")
            val responseBody = jsonObject.getJSONObject("ResponseBody")
            val payloadString = responseBody.optNullableString("Payload")
            val responseBodyString = jsonObject.getString("ResponseBody")

            when(apiName){
                APIObject.DeviceList.route -> {
                    val resBody = Gson().fromJson(responseBodyString, DeviceListResponseBody::class.java)
                    val deviceList = resBody.Devices?:return@AWSIotMqttNewMessageCallback
                    deviceListPrepare(deviceList)
                }
                APIObject.GetUserSync.route -> {
                    val payload = Gson().fromJson(payloadString, ResponsePayload::class.java)
                    /**Cloud will response one list one time.*/
                    val version = responseBody.getJSONObject("Payload").getJSONObject("Dataset").optNullableJSONObject("DeviceOrder")?.getInt("version")
                    version?.let {
                        userSync.updateOrderModifyVersion(version)
                    }
                    orderListPrepare(payload.Dataset.DeviceOrder?.Order, payload.Dataset.BLEDevices?.Devices)
                }
                APIObject.UpdateUserSync.route -> {
                    val version = responseBody.getJSONObject("Payload").getJSONObject("Dataset").optNullableJSONObject("DeviceOrder")?.getInt("version")
                    version?.let {
                        userSync.updateOrderModifyVersion(version)
                    }
//                    Timber.d(message)
                }
                APIObject.DeviceProvision.route -> {
                    val thingName = responseBody.optNullableString("ThingName")?:throw IllegalArgumentException("ThingName is Null.")
                    provisionDomain.provisionThingName = thingName
                    Timber.d("Now you can set provision of thing-$thingName.")
                }
                APIObject.UpdateDeviceRegistry.route -> {
//                    Timber.d(message)
                }
            }

        } catch (e: Exception) {
            Timber.d("Message encoding error: $e.")
        }
    }

    private fun getUserSync() {
        flow { emit(getIdToken().single()) }
            .map { idToken ->
                val identityId = getIdentityId().single()
                (idToken to identityId)
            }
            .map { (idToken, identityId) ->
                userSync.pubGetUserSync(idToken, identityId, getUuid.invoke())
            }.flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(loadingMessage = "UserSync...") } }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }

    private fun orderListPrepare(orderList: List<UserSyncOrder>?, bleLockList: List<BleLock>?) {
        Timber.tag("orderListPrepare").d(orderList.toString())
        var isBleLockListNeedUpdate = false
        var isOrderListNeedUpdate = false
        orderList?.let {
            deviceOrderList.clear()
            deviceOrderList.addAll(it)
            isOrderListNeedUpdate = true
        }

        bleLockList?.let {
            bleLocksList.clear()
            bleLocksList.addAll(it)
            isBleLockListNeedUpdate = true
            storeBleLocks(it)
        }
        Timber.tag("@").d("compareWithDevList >> orderList = $deviceOrderList, wifiLocksList = $wifiLocksList, bleLocksList = $bleLocksList")
        compareWithDevList(deviceOrderList, wifiLocksList, isBleLockListNeedUpdate, isOrderListNeedUpdate)
    }

    private fun deviceListPrepare(deviceList: List<DeviceThing>) {
        val wifiLockList = mutableListOf<WiFiLock>()
        deviceList.forEach { deviceThing ->
            val wifilock = WiFiLock(
                ThingName = deviceThing.ThingName,
                Attributes = deviceThing.Attributes,
                LockState = Reported(0,0,0,0,0,"","unknown",0,false)
            )
            wifiLockList.add(wifilock)
        }
        wifiLocksList.clear()
        wifiLocksList.addAll(wifiLockList)
        flowForGetThingShadow(wifiLocksList)
    }

    private fun flowForGetThingShadow(list: List<WiFiLock>) = flow { emit (
        list.forEach {
            subPubGetThingShadow(it.ThingName)
            subPubUpdateThingShadow(it.ThingName)
        }
    )}
        .flowOn(Dispatchers.IO)
        .catch { Timber.tag("flowForGetThingShadow").e(it) }
        .launchIn(viewModelScope)

    fun setCurrentPage(page: Int) {
        Timber.d("page:$page")
        if ( page == uiState.value.currentPage )return
        currentBleLock?.disconnect()
        _uiState.update { it.copy(currentPage = page) }
        currentIndex = page
    }

    private fun subPubGetThingShadow(thingName: String) =
        flow {
                emit(getDeviceList.subGetThingShadow(thingName, getUuid.invoke(), callbackMqttGetThingShadow(thingName)))
            }
            .flowOn(Dispatchers.IO)
            .onStart { Timber.d("start to subGetThingShadow") }
            .onCompletion { Timber.d("subGetThingShadow finish...") }
            .catch { Timber.tag("subPubGetThingShadow").e(it) }
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
            Timber.d("Message encoding error: $e.")
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
                Timber.d("Thing-${thingName.substring(0,3)} callbackMqttUpdateThing: $shadowReported")
                Timber.d("Lock Deadbolt: ${shadowReported.Deadbolt}, Lock Connected: ${shadowReported.Connected}")
                updateWifiLockListByMqtt(thingName, shadowReported)
            }
        } catch (e: Exception) {
            Timber.d("Message encoding error: $e.")
        }
    }

    private fun updateWifiLockListByMqtt(deviceIdentity: String, lockState: Reported){
        wifiLocksList.indexOfFirst { it.ThingName == deviceIdentity }.let { index ->
            wifiLocksList[index] = wifiLocksList[index].copy( LockState = lockState )

            if(wifiLocksList[index].LockState.Deadbolt.getLockState() != previousDeadbolt || !wifiLocksList[index].LockState.Connected){
                val loadingList = uiState.value.loadingLocks
                loadingList.removeIf { it.DeviceIdentity == deviceIdentity }
                _uiState.update { it.copy(loadingLocks = loadingList) }
            }
        }
        if(wifiLocksList.all { it.LockState.Deadbolt != "" } || wifiLocksList.isEmpty()) {
            getUserSync()
        }
    }
    private fun createListForView(){
        Timber.d("createListForView")
        val newList = mutableListOf<SunionLock>()

        val orderList = deviceOrderList
//        orderList.sortBy { it.Order }

        orderList.forEach { order ->
            newList.add(
                when(mapDeviceType(order.DeviceType)){
                    DeviceType.WiFi.typeNum -> {
                        val wiFiLock = wifiLocksList.find { it.ThingName == order.DeviceIdentity }?:throw Exception("WiFiLock's info is null.")
                        SunionLock(DeviceIdentity = order.DeviceIdentity, wiFiLock.Attributes, wiFiLock.LockState, null, DeviceType.WiFi.typeNum, order.Order)
                    }
                    DeviceType.Ble.typeNum -> {
                        val bleLock = bleLocksList.find { it.MACAddress == order.DeviceIdentity }?:throw Exception("BleLock's info is null.")
                        if(bleLock.MACAddress == currentBleLock?.lockInfo?.macAddress){
                            SunionLock(DeviceIdentity = order.DeviceIdentity, null, currentBleLockState, bleLock, DeviceType.Ble.typeNum, order.Order)
                        }
                        else SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.Ble.typeNum, order.Order)
                    }
                    DeviceType.BleMode.typeNum -> {
                        val bleLock = bleLocksList.find { it.MACAddress == order.DeviceIdentity }?:throw Exception("BleLock's info is null.")
                        if(bleLock.MACAddress == currentBleLock?.lockInfo?.macAddress){
                            SunionLock(DeviceIdentity = order.DeviceIdentity, null, currentBleLockState, bleLock, DeviceType.Ble.typeNum, order.Order)
                        }
                        else SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.BleMode.typeNum, order.Order)
                    }
                    else -> { return@forEach }
                }
            )
        }
        //update ui state
//        newList.sortBy { it.Order }
        _uiState.update { it.copy(locks = newList, isLoading = false) }
    }

    private fun compareWithDevList(
        orderList: MutableList<UserSyncOrder>,
        wifiLocksList: MutableList<WiFiLock>,
        isBleLockListNeedUpdate: Boolean,
        isOrderListNeedUpdate: Boolean,
    ){
        val updateByWifiLocksList =
            if ( isOrderListNeedUpdate || wifiLocksList.isNotEmpty()) {
                orderList.filter { it.DeviceType == "wifi" }.let { theOrderList ->
                    if (theOrderList.size != wifiLocksList.size) true
                    else if (wifiLocksList.any { wifiLock -> theOrderList.none { it.DeviceIdentity == wifiLock.ThingName } }) true
                    else wifiLocksList.any { wifiLock -> theOrderList.find { it.DeviceIdentity == wifiLock.ThingName }?.DisplayName != wifiLock.Attributes.DeviceName }
                }
            }else false

        val updateByBleLocksList =
            if ( isBleLockListNeedUpdate || isOrderListNeedUpdate ) {
                orderList.filter { it.DeviceType != "wifi" }.let { theOrderList ->
                    if (theOrderList.size != bleLocksList.size) true
                    else if (bleLocksList.any { bleLock -> theOrderList.none { it.DeviceIdentity == bleLock.MACAddress } }) true
                    else bleLocksList.any { bleLock -> theOrderList.find { it.DeviceIdentity == bleLock.MACAddress }?.DisplayName != bleLock.DisplayName }
                }
            }else false

        if ( updateByWifiLocksList || updateByBleLocksList ){
            updateOrderAfterCompareWithDevList(orderList, wifiLocksList, bleLocksList)
        }else {
            createListForView()
            _uiState.update { it.copy(lockOrder = orderList) }
        }
    }

    private fun updateOrderAfterCompareWithDevList(
        orderList: MutableList<UserSyncOrder>,
        wifiLocksList: MutableList<WiFiLock>,
        bleLocksList: MutableList<BleLock>
    ){
        Timber.d("updateOrderAfterCompareWithDevList")
        flow { emit(createNewOrderList(orderList, wifiLocksList, bleLocksList)) }
            .map { ( newOrderList, version )->

                userSync.updateUserSync(getUuid.invoke(), UserSyncRequestPayload(
                    Dataset = RequestDataset(
                        DeviceOrder = RequestOrder(newOrderList, userSync.orderModifierVersion.value),
                        BLEDevices = null
                    )
                ))
            }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }

    private fun createNewOrderList(
        orderList: MutableList<UserSyncOrder>,
        wifiLocksList: MutableList<WiFiLock>,
        bleLocksList: MutableList<BleLock>
    ): Pair<MutableList<UserSyncOrder>,Int>{
        val newOrderList = mutableListOf<UserSyncOrder>()

        newOrderList.addAll(orderList)

        bleLocksList.forEach { bleLock ->
            //bleLock list 有的，order list 也要有 (do in pairing)
//                    if(orderData?.Order?.filter { it.DeviceIdentity == bleLock.MACAddress }.isNullOrEmpty()){
//                        newOrderList?.add(UserSyncOrder(
//                            DeviceIdentity = bleLock.MACAddress,
//                            DeviceType = if(bleLock.M.equals("KDW00"))"ble mode" else "ble", DisplayName = bleLock.DisplayName, Order = 0),
//                        )
//                    }
            //bleLock list 名子不同的，order list 也要改
//            if(orderList.find { it.DeviceIdentity == bleLock.MACAddress }?.DisplayName != bleLock.DisplayName){
                newOrderList.find { it.DeviceIdentity == bleLock.MACAddress }?.DisplayName = bleLock.DisplayName
//            }
        }
        wifiLocksList.forEach { wifiLock ->
            //device list 有的，order list 也要有
            if(orderList.filter { it.DeviceIdentity == wifiLock.ThingName }.isEmpty()){
                newOrderList.add(UserSyncOrder(
                    DeviceIdentity = wifiLock.ThingName,
                    DeviceType = "wifi", DisplayName = wifiLock.Attributes.DeviceName, Order = 0),
                )
            }
            //device list 名子不同的，order list 也要改
            if(orderList.find { it.DeviceIdentity == wifiLock.ThingName }?.DisplayName != wifiLock.Attributes.DeviceName){
                newOrderList.find { it.DeviceIdentity == wifiLock.ThingName }?.DisplayName = wifiLock.Attributes.DeviceName?:throw Exception("DeviceNameNull")
            }
        }
        //bleLock list 沒有的，order list 也不能有，但不能刪到wifiLock
        orderList.filter { it.DeviceType != "wifi" }.forEach { order ->
            if(bleLocksList.none { it.MACAddress == order.DeviceIdentity }){
                newOrderList.let { list ->
                    list.remove(list.find { it.DeviceIdentity == order.DeviceIdentity })
                }
            }
        }
        //device list 沒有的，order list 也不能有，但不能刪到bleLock
        orderList.filter { it.DeviceType == "wifi" }.forEach { order ->
            if(wifiLocksList.none { it.ThingName == order.DeviceIdentity }){
                newOrderList.let { list ->
                    list.remove(list.find { it.DeviceIdentity == order.DeviceIdentity })
                }
            }
        }
        Timber.d("prepare newOrderList newOrderList = $newOrderList")
        return newOrderList to userSync.orderModifierVersion.value
    }

    sealed class DeviceType(val typeNum: Int) {
        object WiFi : DeviceType(1)
        object Ble : DeviceType(2)
        object BleMode : DeviceType(3)
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
                userSync.pubUpdateOrderList(idToken, identityId, getUuid.invoke(), tempOrderList)
            }
            .flowOn(Dispatchers.IO)
            .onStart { _uiState.update { it.copy(isLoading = true, loadingMessage = "List ReOrder.") } }
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

        if(lock.LockType != DeviceType.WiFi.typeNum) {
            if(uiState.value.loadingLocks.contains(lock))return
            //ble connect
            val loadingList = uiState.value.loadingLocks
            loadingList.add(lock)
            _uiState.update { it.copy(loadingLocks = loadingList) }
            clickBleLock(lock)
        }else {
            previousDeadbolt = lock.LockState?.Deadbolt?.getLockState()?:return
            if(!lock.LockState.Connected || uiState.value.loadingLocks.contains(lock))return

            val loadingList = uiState.value.loadingLocks
            loadingList.add(lock)
            _uiState.update { it.copy(loadingLocks = loadingList) }
            clickWiFiLock(lock)
        }
    }

    private fun clickBleLock(lock: SunionLock) {
        if (currentBleLock?.isConnected() != true){
            flow { emit(lockProvider.getLockByMacAddress(lock.BleLockInfo?.MACAddress?: throw Exception("MACAddressNull"))) }
                .map {
                    currentBleLock = (it?:throw Exception("null"))as AllLock
                    delay(1000) //wait for lock disconnect.
                }
                .map {
                    currentBleLock?.connect()
                }
                .catch { e -> toastHttpException(e) }
                .flowOn(Dispatchers.IO)
                .launchIn(viewModelScope)
        }else{

            if(lock.LockState?.Direction == "unknown"){
                Timber.d("ready to getBoltOrientation")
                flow { emit(currentBleLock?.getBoltOrientationByBle()) }
                    .map {
                        updateBleLockBySetting(it, lock)
                    }
                    .catch { e -> toastHttpException(e) }
                    .flowOn(Dispatchers.IO)
                    .launchIn(viewModelScope)
            }else {
                if (lock.LockState?.Deadbolt == "unlock") {
                    Timber.d("ready to lock")
                    flow { emit(currentBleLock?.lockByBle()) }
                        .map {
                            updateBleLockBySetting(it, lock)
                        }
                        .catch { e -> toastHttpException(e) }
                        .flowOn(Dispatchers.IO)
                        .launchIn(viewModelScope)
                }
                else {
                    Timber.d("ready to unlock")
                    flow { emit(currentBleLock?.unlockByBle()) }
                        .map {
                            updateBleLockBySetting(it, lock)
                        }
                        .catch { e -> toastHttpException(e) }
                        .flowOn(Dispatchers.IO)
                        .launchIn(viewModelScope)
                }
            }
        }
    }

    private fun collectLockSetting(){
        bleLockState
            .onEach {
                updateBleLockBySetting(it, uiState.value.locks.find { it.DeviceIdentity == currentBleLock?.lockInfo?.macAddress }?:throw Exception("currentBleLockNull"))
            }
            .catch { e -> Timber.tag("CollectLockSetting").e(e) }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun updateBleLockBySetting(setting: LockSetting?, lock: SunionLock) {
        Timber.tag("ready to update UI").d(setting.toString())
        val deadbolt = if (setting?.status?.equals(LockStatus.UNLOCKED) == true) "unlock" else "lock"
        val orientation = when (setting?.config?.orientation){
            LockOrientation.Right -> "right"
            LockOrientation.Left -> "left"
            else -> "unknown"
        }
        currentBleLockState = currentBleLockState.copy(
            Direction = orientation,
            Deadbolt = deadbolt,
            Connected = true,
            Battery = setting?.battery?:0
        )

        val newList = mutableListOf<SunionLock>()
        val list = uiState.value.locks
        list.forEach { oldlock ->
            newList.add(
                if(oldlock.DeviceIdentity == lock.DeviceIdentity){
                    SunionLock(
                        oldlock.DeviceIdentity,
                        oldlock.Attributes,
                        LockState = currentBleLockState,
                        oldlock.BleLockInfo,
                        oldlock.LockType,
                        oldlock.Order,
                    )
                }else oldlock
            )
        }
        _uiState.update { it.copy(locks = newList) }
        val loadingList = uiState.value.loadingLocks
        if(loadingList.size > 0) loadingList.removeIf { it.LockType != DeviceType.WiFi.typeNum }
        _uiState.update { it.copy(loadingLocks = loadingList) }
        Timber.d(newList.toString())
    }

    private fun collectBleLockStateToShow() {
        bleConnectionState
            .onEach { event ->
                if(event.data?.second?.isNotBlank() == true){

                    /** BleLock State Observer*/
                    flow { emit(collectLockSetting()) }
                        .flatMapConcat {
                            Timber.d(" start to getLockSetting")
                            flow { emit(currentBleLock?.getLockSetting()) }
                        }
                        .flatMapConcat {
                            it?:throw Exception("settingNull")
                        }
                        .map { setting ->
                            Timber.d("Normal Connect.")
                            val sunionLock = uiState.value.locks.find {
                                it.DeviceIdentity == (currentBleLock?.lockInfo?.macAddress
                                    ?: throw Exception("macAddressNull"))
                            }
                            updateBleLockBySetting(setting, sunionLock?: throw Exception("sunionLockNull"))
                            val loadingList = uiState.value.loadingLocks
                            loadingList.removeIf {it.LockType != DeviceType.WiFi.typeNum}
                            _uiState.update { it.copy(loadingLocks = loadingList) }
                        }
                        .catch { e -> Timber.tag("CollectLockSetting").e(e) }
                        .flowOn(Dispatchers.IO)
                        .launchIn(viewModelScope)
                }
                if(event.data?.first == false){
                    Timber.d("Disconnect Ble")
                    val loadingList = uiState.value.loadingLocks
                    loadingList.removeIf {it.LockType != DeviceType.WiFi.typeNum}
                    _uiState.update { it.copy(loadingLocks = loadingList) }
                    val newList = mutableListOf<SunionLock>()
                    val list = uiState.value.locks
                    list.forEach { oldlock ->
                        newList.add(
                            if(oldlock.LockType != DeviceType.WiFi.typeNum){
                                SunionLock(
                                    oldlock.DeviceIdentity,
                                    oldlock.Attributes,
                                    LockState = null,
                                    oldlock.BleLockInfo,
                                    oldlock.LockType,
                                    oldlock.Order,
                                )
                            }else oldlock
                        )
                    }
                    _uiState.update { it.copy(locks = newList) }
                    currentBleLockState = currentBleLockState.copy(Connected = false)
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun storeBleLocks(list: List<BleLock>) {
        list.forEach { lock ->
            flow { emit(updateBleLockFromDB(lock)) }
                .flowOn(Dispatchers.IO)
                .launchIn(viewModelScope)
        }
    }

    private fun updateBleLockFromDB(lock: BleLock) {
        lockInformationRepository.get(lock.MACAddress)
            .doOnSuccess { lockConnection ->
                lockInformationRepository.save(lockConnection.copy(deviceName = lock.DisplayName, permanentToken = lock.PermanentToken))
                Timber.d("update key two successful")
            }
            .doOnError {
                saveNewLock(lock)
            }
            .subscribeOn(Schedulers.single())
            .subscribe({
                Timber.d("$it has been store.")
            },{
                Timber.e(it)
            })
    }

    private fun saveNewLock(lock: BleLock) {
        val displayIndex =
            runCatching {
                lockInformationRepository.getMinIndexOf().blockingGet()
            }.getOrNull()
                ?.run { this - 2 } ?: 0
        lockInformationRepository.save(
            LockConnectionInformation(
                macAddress = lock.MACAddress,
                model = "",
                displayName = lock.DisplayName,
                keyOne = lock.ConnectionKey,
                keyTwo = "",
                oneTimeToken = lock.OneTimeToken,
                permanentToken = lock.PermanentToken,
                isOwnerToken = true,
                tokenName = lock.SharedFrom,
                createdAt = 1614298596650,
                permission = "A",
                index = displayIndex
            )
        )
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
                        if(bleLock.MACAddress == currentBleLock?.lockInfo?.macAddress){
                            SunionLock(DeviceIdentity = order.DeviceIdentity, null, currentBleLockState, bleLock, DeviceType.Ble.typeNum, order.Order)
                        }
                        else SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.Ble.typeNum, order.Order)
                    }
                    DeviceType.BleMode.typeNum -> {
                        val bleLock = bleLocksList.find { it.MACAddress == order.DeviceIdentity }?:throw Exception("BleLock's info is null.")
                        if(bleLock.MACAddress == currentBleLock?.lockInfo?.macAddress){
                            SunionLock(DeviceIdentity = order.DeviceIdentity, null, currentBleLockState, bleLock, DeviceType.Ble.typeNum, order.Order)
                        }
                        else SunionLock(DeviceIdentity = order.DeviceIdentity, null, null, bleLock, DeviceType.BleMode.typeNum , order.Order)
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
    fun setLockName(name: String){
        _uiState.update { it.copy(tempLockName = name) }
        Timber.d(name)
    }
    fun saveName(thingName: String){
        uiState.value.locks.find { it.DeviceIdentity == thingName }?.let { device ->
            if(device.LockType == DeviceType.WiFi.typeNum){
                Timber.d("new name = ${device.Attributes?.DeviceName}")
                flow { emit(iotService.updateDeviceName(thingName, uiState.value.tempLockName, getUuid.invoke())) }
                    .flatMapConcat { flow { emit(getDeviceList())} }
                    .catch { e -> toastHttpException(e) }
                    .onStart { _uiState.update { it.copy(isLoading = true, loadingMessage = "Updating...") } }
                    .flowOn(Dispatchers.IO)
                    .launchIn(viewModelScope)
            }else{
                //
                flow { emit(delay(1000)) }
                    .flatMapConcat { flow { emit(lockProvider.getLockByMacAddress(thingName)) } }
                    .flatMapConcat {
                        flow { emit(it?.changeLockNameByBle(uiState.value.tempLockName)) }
                    }
                    .flatMapConcat {
                        flow { emit(updateUserSyncWithBleName(uiState.value.tempLockName)) }
                    }
                    .flowOn(Dispatchers.IO)
                    .onStart {
                        _uiState.update { it.copy(isLoading = true, loadingMessage = "Updating...") }
                    }
                    .catch { Timber.e(it) }
                    .launchIn(viewModelScope)
            }
        }
    }

    fun getDeviceList(){
        flow { emit(getIdToken().single()) }
            .map { idToken ->
                val identityId = getIdentityId().single()
                (idToken to identityId)
            }
            .map { (idToken, identityId) ->
                getDeviceList.pubDeviceList(idToken, identityId, getUuid.invoke())
            }.flowOn(Dispatchers.IO)
//            .onStart { _uiState.update { it.copy(isLoading = true, loadingMessage = "Updating...") } }
            .catch { e -> toastHttpException(e) }
            .launchIn(viewModelScope)
    }

    fun turnToTheLastPage(){
        flow { emit(lockProvider.getLockByMacAddress(uiState.value.locks.last().BleLockInfo?.MACAddress?: throw Exception("MACAddressNull"))) }
            .map {
                currentBleLock = it as AllLock
                Timber.d("current lock's mac = ${it.lockInfo.macAddress}")
                statefulConnection.emitConnectStateAfterPairing()
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)

        _uiState.update { it.copy(currentPage = it.locks.size + 1) }
    }

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
    val tempLockName: String = "",
    var lockOrder: MutableList<UserSyncOrder> = mutableStateListOf(),
    val currentPage: Int = 0,
    val networkAvailable: Boolean = false,
)

sealed class HomeUiEvent {
    object BoltOrientationSuccess : HomeUiEvent()
    object BoltOrientationFailed : HomeUiEvent()
}