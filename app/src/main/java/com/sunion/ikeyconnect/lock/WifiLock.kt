package com.sunion.ikeyconnect.lock

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.LockEventLogRepository
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.Interface.UserCodeRepository
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.exception.EmptyLockInfoException
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.model.LockInfo
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class WifiLock @Inject constructor(
    private val lockInformationRepository: LockInformationRepository,
    private val userCodeRepository: UserCodeRepository,
    private val eventLogRepository: LockEventLogRepository,
    private val statefulConnection: ReactiveStatefulConnection
    ) : Lock {

    private var connectionDisposable: Disposable? = null

    private var mLock: LockConnectionInformation? = null

    private var _lockInfo: LockInfo? = null
    override val lockInfo: LockInfo
        get() = _lockInfo ?: throw EmptyLockInfoException()

    private val _connectionState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connectionState: SharedFlow<Event<Pair<Boolean, String>>> = _connectionState

    fun init(lockInfo: LockInfo): WifiLock {
        this._lockInfo = lockInfo
        val disposable = lockInformationRepository.get(lockInfo.macAddress)
            .map {
                mLock = it
            }
        return this
    }

    override fun connect() {
            statefulConnection
                .device(lockInfo?.macAddress?:return)
                .let {
//                    mRxBleDevice.value = it
                    val disposable = it.establishConnection(false)
                        .flatMap { rxConnect ->
//                            viewModelScope.launch {
//                                mRxBleConnection.value = rxConnect
//                            }
                            statefulConnection.connectWithToken(mLock?:throw EmptyLockInfoException(), rxConnect)
                        }
                        .subscribe({
                            //only user has all permission can continue next step , need to edit todo
//                            Log.d("TAG","Success connect with $it Permission.")
//                            if(it.equals("A")){
//                                success.invoke()
//                                viewModelScope.launch {
//                                    mLockBleStatus.value = BleStatus.CONNECT
//                                }
//                            }
                        },{
                            Timber.e(it.toString())
//                            failure(it)
//                            viewModelScope.launch {
//                                mLockBleStatus.value = BleStatus.UNCONNECT
//                            }
                        })
                    connectionDisposable = disposable
                }
//        (mRxBleDevice.value?:return).observeConnectionStateChanges()
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { onConnectionStateChange(it) }
//            .let { stateDisposable = it }

    }

    override fun disconnect() {
        connectionDisposable?.dispose()
    }

    override fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getName(shouldSave: Boolean): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun setTime(epochSecond: Long): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun getLockName(): String {
        TODO("Not yet implemented")
    }

    override suspend fun hasAdminCodeBeenSet(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun changeLockName(name: String, clientToken: String?): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setTimeZone(timezone: String, clientToken: String?): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun changeAdminCode(
        code: String,
        userName: String,
        timezone: String,
        clientToken: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun editToken(index: Int, permission: String, name: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun delete(clientToken: String?) {
        TODO("Not yet implemented")
    }

//    suspend fun deviceProvisionCreate(
//        serialNumber: String,
//        deviceName: String,
//        timeZone: String,
//        timeZoneOffset: Int,
//        clientToken: String,
//        model: String,
//    ): Boolean {
//        return iotService.deviceProvisionCreate(
//            serialNumber,
//            deviceName,
//            timeZone,
//            timeZoneOffset,
//            clientToken,
//            model
//        )
//    }
}