package com.sunion.ikeyconnect.domain.usecase.device

import android.util.Base64
import com.sunion.ikeyconnect.Scheduler
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import com.sunion.ikeyconnect.domain.exception.NotConnectedException
import com.sunion.ikeyconnect.domain.model.LockConfig
import com.sunion.ikeyconnect.domain.model.LockSetting
import com.sunion.ikeyconnect.domain.toHex
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockSettingUseCase @Inject constructor(
    private val scheduler: Scheduler,
    private val iKeyDataTransmission: BleCmdRepository,
    private val lockInformationRepository: LockInformationRepository,
    private val statefulConnection: StatefulConnection
) : LockSettingDomain {

    override fun getLockSetting(): Single<LockSetting> {
        return statefulConnection.connectMacAddress?.let { mac ->
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    statefulConnection.sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xD6,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT)
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
        } ?: throw NotConnectedException()
    }

    override fun setConfig(setting: LockConfig): Single<Boolean> {
        return statefulConnection.connectMacAddress?.let { mac ->
            val bytes = iKeyDataTransmission.settingBytes(setting)
            Timber.d("setting: ${bytes.toHex()}")
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    statefulConnection.sendCommandThenWaitSingleNotification(
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
                            lockInformationRepository
                                .get(mac)
                                .doOnSuccess { lockConnection ->
                                    lockInformationRepository.save(
                                        lockConnection.copy(
                                            latitude = setting.latitude,
                                            longitude = setting.longitude
                                        )
                                    )
                                }
                                .subscribeOn(scheduler.io())
                                .subscribe()
                        }
                }
        } ?: throw NotConnectedException()
    }

    override fun queryConfig(): Single<LockConfig> {
        return statefulConnection.connectMacAddress?.let { mac ->
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    statefulConnection.sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xD4,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT)
                        )
                    )
                        .filter { notification ->
                            iKeyDataTransmission.decrypt(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                            )?.let { decrypted ->
                                if (decrypted.component3().unSignedInt() == 0xEF) {
                                    throw LockStatusException.AdminCodeNotSetException()
                                } else decrypted.component3().unSignedInt() == 0xD4
                            } ?: false
                        }
                        .take(1)
                        .singleOrError()
                        .flatMap { notification ->
                            Single.just(
                                iKeyDataTransmission.resolveD4(
                                    Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                    notification
                                )
                            )
                        }
                }
        } ?: throw NotConnectedException()
    }

    override fun setVacationMode(isOn: Boolean): Single<LockConfig> {
        return queryConfig()
            .flatMap { config -> setConfig(config.copy(isVacationModeOn = isOn)) }
            .flatMap { isSuccessful ->
                if (isSuccessful) {
                    queryConfig()
                } else {
                    Single.error(RuntimeException())
                }
            }
    }

    override fun setAutoLock(isOn: Boolean, value: Int): Single<LockConfig> {
        return queryConfig()
            .flatMap { config ->
                Timber.d("autoLockTime: $value")
                setConfig(
                    config.copy(
                        isAutoLock = isOn,
                        autoLockTime = value
                    )
                )
            }
            .flatMap { isSuccessful ->
                if (isSuccessful) {
                    queryConfig()
                } else {
                    Single.error(RuntimeException())
                }
            }
    }

    override fun setPreamble(isOn: Boolean): Single<LockConfig> {
        return queryConfig()
            .flatMap { config -> setConfig(config.copy(isPreamble = isOn)) }
            .flatMap { isSuccessful ->
                if (isSuccessful) {
                    queryConfig()
                } else {
                    Single.error(RuntimeException())
                }
            }
    }

    override fun setKeyPressBeep(isOn: Boolean): Single<LockConfig> {
        return queryConfig()
            .flatMap { config -> setConfig(config.copy(isSoundOn = isOn)) }
            .flatMap { isSuccessful ->
                if (isSuccessful) {
                    queryConfig()
                } else {
                    Single.error(RuntimeException())
                }
            }
    }

    override fun setLocation(latitude: Double, longitude: Double): Single<LockConfig> {
        return statefulConnection.connectMacAddress?.let { mac ->
            queryConfig()
                .flatMap { config ->
                    Timber.d("current lat: ${config.latitude} lng: ${config.longitude}, new lat: $latitude, new lng: $longitude")
                    setConfig(config.copy(latitude = latitude, longitude = longitude))
                }
                .flatMap { isSuccessful ->
                    if (isSuccessful) {
                        queryConfig()
                    } else {
                        Single.error(RuntimeException())
                    }
                }
        } ?: Single.error(NotConnectedException())
    }
}

interface LockSettingDomain {
    fun setVacationMode(isOn: Boolean): Single<LockConfig>
    fun setAutoLock(isOn: Boolean, value: Int): Single<LockConfig>
    fun setKeyPressBeep(isOn: Boolean): Single<LockConfig>
    fun setLocation(latitude: Double, longitude: Double): Single<LockConfig>
    fun setConfig(setting: LockConfig): Single<Boolean>
    fun getLockSetting(): Single<LockSetting>
    fun queryConfig(): Single<LockConfig>
    fun setPreamble(isOn: Boolean): Single<LockConfig>
}

