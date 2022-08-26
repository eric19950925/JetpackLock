package com.sunion.ikeyconnect.auto_unlock

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.GeofencingEvent
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.Scheduler
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import com.sunion.ikeyconnect.domain.exception.ConnectionTokenException
import com.sunion.ikeyconnect.domain.exception.EmptyLockInfoException
import com.sunion.ikeyconnect.domain.model.Event
import com.sunion.ikeyconnect.domain.model.EventState
import com.sunion.ikeyconnect.domain.model.LockStatus
import com.sunion.ikeyconnect.domain.usecase.UseCase
import com.sunion.ikeyconnect.domain.usecase.device.BleScanUseCase
import com.sunion.ikeyconnect.domain.usecase.device.LockSettingUseCase
import com.sunion.ikeyconnect.domain.usecase.device.ToggleLockUseCase
import com.sunion.ikeyconnect.home.HomeViewModel
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

typealias ActionFromService = () -> Unit

@Singleton
class AutoUnlockUseCase @Inject constructor(
    private val scheduler: Scheduler,
    private val lockSettingUseCase: LockSettingUseCase,
    private val toggleLockUseCase: ToggleLockUseCase,
    private val lockInformationRepository: LockInformationRepository,
    private val statefulConnection: StatefulConnection,
    private val bleScanUseCase: BleScanUseCase,
    private val lockProvider: LockProvider,
) : UseCase.JustExecute<ActionFromService> {

    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bleConnectionState: SharedFlow<Event<Pair<Boolean, String>>> = statefulConnection.connectionState

    private fun collectBleLockState() {
        bleConnectionState
            .onEach { connectionEvent ->
                onLockConnect(connectionEvent)
            }
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(it) }
            .launchIn(lockScope)
    }

    val geofenceEventLiveData = MutableLiveData<GeofencingEvent>()

    var action: ActionFromService? = null

    val status = PublishSubject.create<AutoUnlockStatus>()

    var mac: String? = null

    override fun invoke(input: ActionFromService) {

        Timber.d("triggeredTime:${geofenceEventLiveData.value?.triggeringLocation?.time}")
        if (statefulConnection.isConnectedWithDevice()) {
            unlock {
                Timber.d("Connected, unlock directly")
                action?.invoke()
                action = null
                lockScope.cancel()
            }
        } else {
            action = input
            collectBleLockState()
            Timber.d("Disconnected, establish connection first, start scanning")
            try{
                flow { emit(lockProvider.getLockByMacAddress(mac ?: throw EmptyLockInfoException())) }
                    .map {
                        // call API to wake wifi lock (就算是 bt lock 也做)
                        it
                    }
                    .map {
                        statefulConnection.connectAsFlowForAutoUnLock(it?.lockInfo ?: throw EmptyLockInfoException())
                    }
                    .catch {
                        Timber.e(it)
                        status.onNext(
                            if (it is TimeoutException) {
                                AutoUnlockStatus.AutoUnlockTimeout(mac ?: "")
                            } else if (it is ConnectionTokenException.IllegalTokenStateException ||
                                it is ConnectionTokenException.DeviceRefusedException
                            ) {
                                AutoUnlockStatus.AutoUnlockConnectionFail(mac ?: "")
                            } else {
                                AutoUnlockStatus.AutoUnlockOperationFail(mac ?: "", it.message ?: "Auto-unlock error occur")
                            }
                        )
                    }
                    .flowOn(Dispatchers.IO)
                    .launchIn(lockScope)
            }catch (e:Exception){Timber.e(e)}
        }
    }

    private fun onLockConnect(connectionEvent: Event<Pair<Boolean, String>>?) {
        Timber.e("reconnect event while auto-unlock received, event status: ${connectionEvent?.status} ${connectionEvent?.data}")
        if (connectionEvent?.status == EventState.SUCCESS && connectionEvent.data?.first == true) {
            Timber.d("connect to lock successful, attempt to unlock the lock")
            unlock {
                action?.invoke()
                action = null
            }
        } else {
            Timber.d("connection event: ${connectionEvent?.data}")
        }

        val message = connectionEvent?.message ?: return

        if (connectionEvent.status == EventState.ERROR &&
            (
                message == ConnectionTokenException.DeviceRefusedException::class.java.simpleName ||
                    message == ConnectionTokenException.IllegalTokenException::class.java.simpleName
                )
        ) {
            Timber.e("connection fail due to token validation")
            mac?.let {
                status.onNext(AutoUnlockStatus.AutoUnlockConnectionFail(it))
            }
        }
    }

    fun unlock(action: () -> Unit) {
        flow { emit(lockSettingUseCase.getLockSetting().toObservable().asFlow().single()) }
            .map {
                if (it.status == LockStatus.LOCKED) {
                    Timber.d("GETTING LockStatus SUCCESS: $it")
                    toggleLockUseCase.invoke(0)
                } else {
                    throw AutoUnlockAlreadyUnlockedException()
                }
            }
            .map {
                action.invoke()
                statefulConnection.connectMacAddress?.let { address ->
                    status.onNext(AutoUnlockStatus.AutoUnlockOperationSuccess(address))
                }
            }
            .catch {
                Timber.e("unlock operation error: $it")
                mac?.let { address ->
                    status.onNext(
                        if (it is AutoUnlockAlreadyUnlockedException) {
                            AutoUnlockStatus.AutoUnlockAlreadyUnlocked(address)
                        } else {
                            AutoUnlockStatus.AutoUnlockOperationFail(address, it.message ?: "")
                        }
                    )
                }
            }
            .launchIn(lockScope)

    }

    fun editAutoUnlockSetting(isOn: Boolean, deviceType: Int): Flow<Any> {
        return if(deviceType == HomeViewModel.DeviceType.WiFi.typeNum){
            flow { emit(delay(1000)) }
        }else{
            statefulConnection.connectMacAddress?.let { mac ->
                Timber.d("mac: $mac")
                lockInformationRepository
                    .get(mac)
                    .map { lockConnection ->
                        lockInformationRepository.save(lockConnection.copy(isAutoUnlockOn = isOn))
                    }
            }?.toObservable()?.asFlow()?:throw Exception("SaveSettingFail")
        }
    }

    class AutoUnlockAlreadyUnlockedException() : Throwable()
}
