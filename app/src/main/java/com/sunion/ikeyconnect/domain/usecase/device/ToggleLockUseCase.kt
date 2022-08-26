package com.sunion.ikeyconnect.domain.usecase.device

import android.util.Base64
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import com.sunion.ikeyconnect.domain.exception.NotConnectedException
import com.sunion.ikeyconnect.domain.model.LockSetting
import com.sunion.ikeyconnect.domain.model.LockStatus
import com.sunion.ikeyconnect.domain.usecase.UseCase
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleLockUseCase @Inject constructor(
    private val iKeyDataTransmission: BleCmdRepository,
    private val lockInformationRepository: LockInformationRepository,
    private val statefulConnection: StatefulConnection
) : UseCase.ReactiveSingle<Int, LockSetting> {
    override fun invoke(input: Int): Single<LockSetting> {
        Timber.d("statefulConnection: $statefulConnection")
        return statefulConnection.connectMacAddress?.let { mac ->
            Timber.d("mac: $mac")
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    statefulConnection.sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xD7,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            if (input == LockStatus.LOCKED) byteArrayOf(0x00) else byteArrayOf(0x01)
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
}
