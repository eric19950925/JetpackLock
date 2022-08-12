package com.sunion.ikeyconnect.lock

import android.util.Base64
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.ikeyconnect.domain.Interface.*
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.BluetoothConnectState
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.command.GetLockConfigCommand
import com.sunion.ikeyconnect.domain.command.WifiConnectState
import com.sunion.ikeyconnect.domain.exception.EmptyLockInfoException
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import com.sunion.ikeyconnect.domain.exception.UserCodeException
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.model.DeviceToken.DeviceTokenState.PERMISSION_ALL
import com.sunion.ikeyconnect.domain.toHex
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.unless
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllLock @Inject constructor(
    private val lockInformationRepository: LockInformationRepository,
    private val userCodeRepository: UserCodeRepository,
    private val eventLogRepository: LockEventLogRepository,
    private val statefulConnection: ReactiveStatefulConnection,
    private val iKeyDataTransmission: BleCmdRepository,
    private val iotService: SunionIotService,
    private val getIdToken: GetIdTokenUseCase,
    ) : Lock {

    private var connectionDisposable: Disposable? = null

    private var mLock: LockConnectionInformation? = null

    private var _lockInfo: LockInfo? = null
    override val lockInfo: LockInfo
        get() = _lockInfo ?: throw EmptyLockInfoException()

    private var bleDevice: RxBleDevice? = null
    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var rxBleConnection: RxBleConnection? = null
    private var scanJob: Job? = null
    private var connectionJob: Job? = null
    private var keyTwo = ""
    private val wifiCmdUuid = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
    private val connectWifiState = mutableListOf<String>()
    private val _connectionState2 = MutableSharedFlow<BluetoothConnectState>()
    val connectionState2: SharedFlow<BluetoothConnectState> = statefulConnection.connectionState2

//    private val _connectionState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connectionState: SharedFlow<Event<Pair<Boolean, String>>> = statefulConnection.connectionState

    fun init(lockInfo: LockInfo): AllLock {
        this._lockInfo = lockInfo
        return this
    }

    override fun connect() {
        statefulConnection.connect_asflow(lockInfo)
//        statefulConnection.establishConnection(lockInfo.macAddress, false)
        Timber.d("find ${lockInfo.deviceName}")
//        statefulConnection.establishConnection_by_deviceName(lockInfo.deviceName, false)
        }

    override fun disconnect() {
        connectionDisposable?.dispose()
        statefulConnection.close()
    }

    override fun isConnected(): Boolean = statefulConnection.getIsConnected()

    override fun getName(shouldSave: Boolean): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun collectLockSetting(): Flow<LockSetting> {
        return lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMapObservable { lockConnection ->
                (statefulConnection.setupNotificationsFor(0xD6))
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            decrypted.component3().unSignedInt() == 0xD6
                        } ?: false
                    }
                    .map { notification ->
                        iKeyDataTransmission.resolveD6(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            notification
                        )
                    }
            }
            .asFlow()
    }

    override fun setTime(epochSecond: Long): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun getLockName(): String {
        TODO("Not yet implemented")
    }

    override suspend fun hasAdminCodeBeenSet(): Boolean = hasAdminCodeBeenSetByBle()

    private suspend fun hasAdminCodeBeenSetByBle(): Boolean {
        val command = iKeyDataTransmission.createCommand(
            function = 0xEF,
            key = Base64.decode(keyTwo, Base64.DEFAULT)
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "hasAdminCodeBeenSetByBle")
            .filter { notification ->
                iKeyDataTransmission.decrypt(
                    Base64.decode(keyTwo, Base64.DEFAULT), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xEF
                } ?: false
            }
            .take(1)
            .map { notification ->
                iKeyDataTransmission.resolveEf(
                    Base64.decode(keyTwo, Base64.DEFAULT),
                    notification
                )
            }
            .single()
    }

    override suspend fun changeLockNameByBle(name: String): Boolean =
        lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMap { lockConnection ->
                statefulConnection
                    .sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xD1,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            name.toByteArray()
                        )
                    )
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            decrypted.component3().unSignedInt() == 0xD1
                        } ?: false
                    }
                    .take(1)
                    .singleOrError()
                    .flatMap { notification ->
                        Single.just(
                            iKeyDataTransmission.resolveD1(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                notification
                            )
                        )
                    }
                    .doOnSuccess {
                        Timber.d("name :$name has been saved to local cache")
                        unless(it) {
                            lockInformationRepository.save(
                                lockConnection.copy(
                                    displayName = name
                                )
                            )
                        }
                    }
            }.toObservable().asFlow().single()

    override suspend fun setTimeZoneByBle(timezone: String): Boolean {
        return lockInformationRepository
            .get(lockInfo.macAddress)
            .map { it.keyTwo }
            .flatMap { keyTwo ->
                val zonedDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of(timezone))
                val offsetSeconds = zonedDateTime.offset.totalSeconds
                val id = ZoneId.of(timezone)
                val offsetByte = iKeyDataTransmission.intToLittleEndianBytes(offsetSeconds)
                val offsetSecondsBytes =
                    offsetSeconds.toString().toCharArray().map { it.code.toByte() }.toByteArray()
                val bytes = offsetByte + offsetSecondsBytes
                Timber.d("set time zone offset $id to: $offsetSeconds, bytes: ${bytes.toHex()}, offsetSecondsBytes: ${offsetSecondsBytes.toHex()}, size ${offsetSecondsBytes.size}")
                statefulConnection
                    .sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xD9,
                            Base64.decode(keyTwo, Base64.DEFAULT),
                            bytes
                        )
                    )
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            decrypted.component3().unSignedInt() == 0xD9
                        } ?: false
                    }
                    .take(1)
                    .singleOrError()
                    .flatMap { notification ->
                        Single.just(
                            iKeyDataTransmission.resolveD9(
                                Base64.decode(keyTwo, Base64.DEFAULT),
                                notification
                            )
                        )
                    }
            }.toObservable().asFlow().single()
    }

    override suspend fun changeAdminCode(
        thingName: String,
        code: String,
        lockName: String,
        timezone: String,
        clientToken: String
    ): Boolean =
        flow { emit(iotService.createAdminCode(thingName, code, clientToken)) }
            .map {
                iotService.updateTimezone(thingName, timezone, clientToken)
            }
            .map {
                //todo user name 是否就是cognito 的 username??
                iotService.updateDeviceName(thingName, lockName, clientToken)
            }
            .map { true }
            .single()

    override suspend fun changeAdminCodeByBle(
        macAddress: String,
        code: String,
        userName: String,
        clientToken: String
    ): Boolean {
        val adminCode = iKeyDataTransmission.stringCodeToHex(code)
        return lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMap { lockConnection ->
                keyTwo = lockConnection.keyTwo
                statefulConnection
                    .sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xC7,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            adminCode
                        )
                    )
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            decrypted.component3().unSignedInt() == 0xC7
                        } ?: false
                    }
                    .take(1)
                    .singleOrError()
                    .map { notification ->
                        iKeyDataTransmission.resolveC7(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            notification
                        )
                    }
                    .flatMap { isAddAdminCodeSuccessful ->
                        if (isAddAdminCodeSuccessful) {
                            Single
                                .timer(400, TimeUnit.MILLISECONDS)
                                .flatMap { updateOwnerTokenName(lockConnection.keyTwo, userName) }
                        } else {
                            throw UserCodeException.AddAdminCodeException()
                        }
                    }
                    .doOnSuccess {
                        lockInformationRepository.save(
                            lockConnection.copy(
                                isOwnerToken = true,
                                tokenName = userName,
                                permission = lockConnection.permission
                            )
                        )
                    }
            }.toObservable().asFlow().single()
    }

    private fun updateOwnerTokenName(keyTwo: String, tokenName: String): Single<Boolean> {
        val bytes = byteArrayOf(0x00) + PERMISSION_ALL.toByteArray() + tokenName.toByteArray()
        Timber.d("token name: $tokenName, e7 bytes: ${bytes.toHex()}")
        return statefulConnection
            .sendCommandThenWaitSingleNotification(
                iKeyDataTransmission.createCommand(
                    0xE7,
                    Base64.decode(keyTwo, Base64.DEFAULT),
                    bytes
                )
            )
            .filter { notification ->
                iKeyDataTransmission.decrypt(
                    Base64.decode(keyTwo, Base64.DEFAULT), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xE7
                } ?: false
            }
            .take(1)
            .singleOrError()
            .map { notification ->
                iKeyDataTransmission.resolveE7(
                    Base64.decode(keyTwo, Base64.DEFAULT),
                    notification
                ).isSuccessful
            }
    }
    override suspend fun editToken(index: Int, permission: String, name: String): Boolean {
        return editTokenByBle(index, permission, name)
    }

    private suspend fun editTokenByBle(index: Int, permission: String, name: String): Boolean {
        val bytes = byteArrayOf(index.toByte()) + permission.toByteArray() + name.toByteArray()
        Timber.d("index: $index, token name: $name, permission: $permission, e7 bytes: ${bytes.toHex()}")
        val command = iKeyDataTransmission.createCommand(
            function = 0xE7,
            key = Base64.decode(keyTwo, Base64.DEFAULT),
            data = bytes
        )

        return statefulConnection.setupSingleNotificationThenSendCommand(command, "editTokenByBle")
            .filter { notification ->
                iKeyDataTransmission.decrypt(
                    Base64.decode(keyTwo, Base64.DEFAULT), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xE7
                } ?: false
            }
            .take(1)
            .map { notification ->
                iKeyDataTransmission.resolveE7(
                    Base64.decode(keyTwo, Base64.DEFAULT),
                    notification
                ).isSuccessful
            }
            .zip(lockInformationRepository.get(lockInfo.macAddress).toObservable().asFlow())
            { isSuccessful, info -> Pair(isSuccessful, info) }
            .map { (isSuccessful, lockConnection) ->
                lockInformationRepository.save(
                    lockConnection.copy(
                        isOwnerToken = true,
                        tokenName = name,
                        permission = lockConnection.permission
                    )
                )
                isSuccessful
            }
            .single()
    }

    override suspend fun setLocation(thingName: String, latitude: Double, longitude: Double, clientToken: String): Boolean {
        return flow { emit(iotService.updateLocation(thingName, latitude, longitude, clientToken)) }
            .map { true }.single()
    }

    override suspend fun setLocationByBle(latitude: Double, longitude: Double): LockConfig {
        val config = getLockConfigByBle()
        Timber.d("current lat: ${config.latitude} lng: ${config.longitude}, new lat: $latitude, new lng: $longitude")
        setConfig(config.copy(latitude = latitude, longitude = longitude))
        return getLockConfigByBle()
    }
    private suspend fun setConfig(setting: LockConfig): Boolean {
        val bytes = iKeyDataTransmission.settingBytes(setting)
        Timber.d("setting: ${bytes.toHex()}")
        return lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMap { lockConnection ->
                statefulConnection
                    .sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xD5,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            bytes
                        )
                    )
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            if (decrypted.component3().unSignedInt() == 0xEF) {
                                throw LockStatusException.AdminCodeNotSetException()
                            } else decrypted.component3().unSignedInt() == 0xD5
                        } ?: false
                    }
                    .take(1)
                    .singleOrError()
                    .flatMap { notification ->
                        Single.just(
                            iKeyDataTransmission.resolveD5(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                notification
                            )
                        )
                    }
                    .doOnSuccess {
                        Timber.d("setConfig:%s", it.toString())
                        lockInformationRepository
                            .get(lockInfo.macAddress)
                            .doOnSuccess { lockConnection ->
                                lockInformationRepository.save(
                                    lockConnection.copy(
                                        latitude = setting.latitude,
                                        longitude = setting.longitude
                                    )
                                )
                            }
//                            .subscribeOn(schedulers.io())
                            .subscribe()
                    }
            }.toObservable().asFlow().single()
    }

    override suspend fun getLockConfigByBle(): LockConfig {
        val command = GetLockConfigCommand(iKeyDataTransmission)
        return statefulConnection.setupSingleNotificationThenSendCommand(
            command.create(keyTwo, Unit),
            "getLockConfigByBle"
        )
            .filter { notification -> command.match(keyTwo, notification) }
            .take(1)
            .map { notification -> command.parseResult(keyTwo, notification) }
            .single()
    }

    override suspend fun getLockConfig(thingName: String, clientToken: String): LockConfig {
        return flow { emit(iotService.getDeviceRegistry(thingName, clientToken)) }
            .map {
                LockConfig(
                    orientation = LockOrientation.NotDetermined,
                    isSoundOn = it.payload.attributes.keyPressBeep,
                    isVacationModeOn = it.payload.attributes.vacationMode,
                    isAutoLock = it.payload.attributes.autoLock,
                    autoLockTime = it.payload.attributes.autoLockDelay,
                    latitude = it.payload.attributes.location.latitude,
                    longitude = it.payload.attributes.location.longitude
                )
            }
            .single()
    }

    override suspend fun delete(clientToken: String?) {
//        if (useWifi && isNetworkConnectedUseCase())
            deleteByNetwork(clientToken)
//        else
//            deleteByBle()
    }
    private fun deleteByNetwork(clientToken: String?) {
        lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMap { lock ->
                lockInformationRepository.delete(lock)
                    .andThen(userCodeRepository.deleteAll(lockInfo.macAddress))
                    .andThen(eventLogRepository.deleteAll(lockInfo.macAddress))
                    .andThen(Single.just(lock.thingName ?: ""))
            }
            .toObservable()
            .asFlow()
            .flatMapConcat { flow { emit(iotService.delete(it, clientToken!!)) } }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(lockScope)
    }

    fun collectWifiList(): Flow<WifiListResult> = statefulConnection.collectWifiList(lockInfo)

    suspend fun scanWifi() = statefulConnection.scanWifi(lockInfo)

    fun collectConnectToWifiState(): Flow<WifiConnectState> = statefulConnection.collectConnectToWifiState(lockInfo)

    suspend fun connectLockToWifi(ssid: String, password: String): Boolean = statefulConnection.connectLockToWifi(ssid, password, lockInfo)

    fun getAndSaveThingName(clientToken: String) =
        flow { emit(iotService.getDeviceList(clientToken)) }
            .map {
                it.find { d -> d.attributes.bluetooth.mACAddress == lockInfo.macAddress }
                    ?.run {
                        lockInformationRepository.setThingName(lockInfo.macAddress, thingName)
                        Timber.d(thingName)
                        thingName
                    }
                    ?: throw Exception("device not found on remote")
            }
            .retry(5) {
                delay(5000)
                true
            }

//    suspend fun lockByNetwork(clientToken: String?): LockSetting {
//
//        val idToken = runCatching {
//            getIdToken().single()
//        }.getOrNull() ?: return LockSetting(LockConfig(LockOrientation.Left,false,false,false,0,null,null),LockStatus.LOCKED,0,0,0)
//
//        return lockInformationRepository
//            .get(lockInfo.macAddress)
//            .toObservable()
//            .asFlow()
//            .flatMapConcat { flow { emit(iotService.lock(it.thingName!!, clientToken!!)) } }
//            .map {
//                LockSetting(
//                    config = LockConfig(
//                        orientation = LockOrientation.Left, //TODO
//                        isSoundOn = false,
//                        isVacationModeOn = false,
//                        isAutoLock = false,
//                        autoLockTime = 0,
//                        latitude = null,
//                        longitude = null
//                    ),
//                    status = LockStatus.LOCKED,
//                    battery = 0,
//                    batteryStatus = 0,
//                    timestamp = 0
//                )
//            }
//            .single()
//    }

}