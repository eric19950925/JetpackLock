package com.sunion.ikeyconnect.lock

import android.util.Base64
import com.sunion.ikeyconnect.domain.Interface.*
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.BluetoothConnectState
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.command.*
import com.sunion.ikeyconnect.domain.exception.EmptyLockInfoException
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import com.sunion.ikeyconnect.domain.exception.UserCodeException
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.domain.model.DeviceToken.DeviceTokenState.PERMISSION_ALL
import com.sunion.ikeyconnect.domain.toHex
import com.sunion.ikeyconnect.domain.usecase.account.GetIdTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.device.SetTimeUseCase
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class  AllLock @Inject constructor(
    private val lockInformationRepository: LockInformationRepository,
    private val userCodeRepository: UserCodeRepository,
    private val eventLogRepository: LockEventLogRepository,
    private val statefulConnection: ReactiveStatefulConnection,
    private val iKeyDataTransmission: BleCmdRepository,
    private val setTime: SetTimeUseCase,
    private val iotService: SunionIotService,
    private val getIdToken: GetIdTokenUseCase,
    ) : Lock {

    private var connectionDisposable: Disposable? = null

    private var _lockInfo: LockInfo? = null
    override val lockInfo: LockInfo
        get() = _lockInfo ?: throw EmptyLockInfoException()

    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var keyTwo = ""
    val connectionState2: SharedFlow<BluetoothConnectState> = statefulConnection.connectionState2

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


    override fun setTime(epochSecond: Long): Flow<Boolean> {
        return setTime.invoke(epochSecond, lockInfo.macAddress).toObservable().asFlow()
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

    override suspend fun setAdminCodeByBle(
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

    override suspend fun changeAdminCodeByBle(
        oldCode: String,
        newCode: String,
    ): Boolean {
        val newBytes = iKeyDataTransmission.stringCodeToHex(newCode)
        val oldBytes = iKeyDataTransmission.stringCodeToHex(oldCode)
        val sendBytes = byteArrayOf(oldBytes.size.toByte()) + oldBytes + byteArrayOf(newBytes.size.toByte()) + newBytes
        return lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMap { lockConnection ->
                keyTwo = lockConnection.keyTwo
                statefulConnection
                    .sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xC8,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            sendBytes
                        )
                    )
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            decrypted.component3().unSignedInt() == 0xC8
                        } ?: false
                    }
                    .take(1)
                    .singleOrError()
                    .map { notification ->
                        iKeyDataTransmission.resolveC8(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            notification
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

    override suspend fun getBoltOrientationByBle(): LockSetting {
        return lockInformationRepository
            .get(lockInfo.macAddress)
            .map {
                Timber.d("lockConnection: $it")
                it.keyTwo
            }
            .flatMap { keyTwo ->
                val key = Base64.decode(keyTwo, Base64.DEFAULT)
                Timber.d("key two: ${key.toHex()}")
                statefulConnection
                    .sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xCC,
                            Base64.decode(keyTwo, Base64.DEFAULT)
                        )
                    )
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            if (decrypted.component3().unSignedInt() == 0xEF) {
                                throw LockStatusException.AdminCodeNotSetException()
                            } else decrypted.component3().unSignedInt() == 0xD6
                        } ?: false
                    }
                    .take(1)
                    .singleOrError()
                    .flatMap { notification ->
                        Single.just(
                            iKeyDataTransmission.resolveD6(
                                Base64.decode(keyTwo, Base64.DEFAULT),
                                notification
                            )
                        )
                    }
                    .map { lockSetting -> lockSetting }
            }.toObservable().asFlow().single()
    }

    override suspend fun getLockSetting(): LockSetting {
        val command = GetLockSettingCommand(iKeyDataTransmission)
        return lockInformationRepository.get(lockInfo.macAddress).toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .map{
                statefulConnection.setupSingleNotificationThenSendCommand(
                    command.create(it.keyTwo, Unit),
                    "getLockSettingByBle"
                )
                    .filter { notification -> command.match(it.keyTwo, notification) }
                    .take(1)
                    .map { notification -> command.parseResult(it.keyTwo, notification) }
                    .single()
            }
            .single()
    }

    suspend fun setConfig(setting: LockConfig): Boolean {
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
        return lockInformationRepository.get(lockInfo.macAddress).toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .map{
                statefulConnection.setupSingleNotificationThenSendCommand(
                    command.create(it.keyTwo, Unit),
                    "getLockConfigByBle"
                )
                    .filter { notification -> command.match(it.keyTwo, notification) }
                    .take(1)
                    .map { notification -> command.parseResult(it.keyTwo, notification) }
                    .single()
            }
            .single()
    }

    suspend fun factoryResetByBle(adminCode: String): Boolean {
        val command = FactoryResetCommand(iKeyDataTransmission)
        return lockInformationRepository.get(lockInfo.macAddress).toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .map{
                statefulConnection.setupSingleNotificationThenSendCommand(
                    command.create2(it.keyTwo, adminCode),
                    "factoryResetByBle"
                )
                    .filter { notification -> command.match(it.keyTwo, notification) }
                    .take(1)
                    .map { notification -> command.parseResult(it.keyTwo, notification) }
                    .single()
            }.single()
    }

    suspend fun getEventQuantity(): Int {
        val command = GetEventQuantityCommand(iKeyDataTransmission)
        return lockInformationRepository.get(lockInfo.macAddress).toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .map {
                statefulConnection.setupSingleNotificationThenSendCommand(
                    command.create(it.keyTwo, Unit),
                    "getEventQuantity"
                )
                    .filter { notification -> command.match(it.keyTwo, notification) }
                    .take(1)
                    .map { notification -> command.parseResult(it.keyTwo, notification) }
                    .single()
            }.single()
    }

    suspend fun getEventByBle(index: Int): Flow<EventLog> {
        val command = GetEventCommand(iKeyDataTransmission)
        return lockInformationRepository.get(lockInfo.macAddress).toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .map{
                statefulConnection.setupSingleNotificationThenSendCommand(
                    command.create2(it.keyTwo, index),
                    "getLockEventLogByBle"
                )
                    .filter { notification -> command.match(it.keyTwo, notification) }
                    .take(1)
                    .map { notification -> command.parseResult(it.keyTwo, notification) }
                    .single()
            }
    }

    suspend fun getTimeZone() {
        val command = GetEventCommand(iKeyDataTransmission)
        lockInformationRepository.get(lockInfo.macAddress).toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .map{
                statefulConnection.setupSingleNotificationThenSendCommand(
                    command.create(it.keyTwo, Unit),
                    "getTimeZone"
                )
                    .filter { notification -> command.match(it.keyTwo, notification) }
                    .take(1)
                    .map { notification -> command.parseResult(it.keyTwo, notification) }
                    .single()
            }
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
                    longitude = it.payload.attributes.location.longitude,
                    isPreamble = it.payload.attributes.preamble
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

    override suspend fun lockByBle(): LockSetting = setLock(LockStatus.LOCKED)

    override suspend fun unlockByBle(): LockSetting = setLock(LockStatus.UNLOCKED)

    private suspend fun setLock(lock: Int): LockSetting {
        return lockInformationRepository
            .get(lockInfo.macAddress)
            .flatMap { lockConnection ->
                statefulConnection.sendCommandThenWaitSingleNotification(
                    iKeyDataTransmission.createCommand(
                        0xD7,
                        Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                        if (lock == LockStatus.UNLOCKED) byteArrayOf(0x00) else byteArrayOf(0x01)
                    )
                )
                    .filter { notification ->
                        iKeyDataTransmission.decrypt(
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                        )?.let { decrypted ->
                            if (decrypted.component3().unSignedInt() == 0xEF) {
                                throw LockStatusException.AdminCodeNotSetException()
                            } else decrypted.component3().unSignedInt() == 0xD6
                        } ?: false
                    }
                    .take(1)
                    .singleOrError()
                    .flatMap { notification ->
                        Single.just(
                            iKeyDataTransmission.resolveD6(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                notification
                            )
                        )
                    }
            }
            .toObservable()
            .asFlow()
            .single()
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