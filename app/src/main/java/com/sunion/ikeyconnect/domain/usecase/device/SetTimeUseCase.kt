package com.sunion.ikeyconnect.domain.usecase.device

import android.util.Base64
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.NotConnectedException
import com.sunion.ikeyconnect.domain.toHex
import io.reactivex.Single
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetTimeUseCase @Inject constructor(
    private val iKeyDataTransmission: BleCmdRepository,
    private val lockInformationRepository: LockInformationRepository,
    private val statefulConnection: StatefulConnection
) {
    fun invoke(input: Long, macAddress: String): Single<Boolean> {
        return lockInformationRepository
                .get(macAddress)
                .flatMap { lockConnection ->
                    val bytes = iKeyDataTransmission.intToLittleEndianBytes(input.toInt())
                    Timber.d("set time to: ${Instant.ofEpochSecond(input)}, bytes: $bytes")
                    statefulConnection
                        .sendCommandThenWaitSingleNotification(
                            iKeyDataTransmission.createCommand(
                                0xD3,
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                bytes
                            )
                        )
                        .filter { notification ->
                            iKeyDataTransmission.decrypt(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                            )?.let { decrypted ->
                                decrypted.component3().unSignedInt() == 0xD3
                            } ?: false
                        }
                        .take(1)
                        .singleOrError()
                        .flatMap { notification ->
                            Single.just(
                                iKeyDataTransmission.resolveD3(
                                    Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                    notification
                                )
                            )
                        }
                }
    }

    fun setTimeZone(zone: String): Single<Boolean> {
        return statefulConnection.macAddress?.let { mac ->
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    val zonedDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of(zone))
                    val offsetSeconds = zonedDateTime.offset.totalSeconds
                    val id = ZoneId.of(zone)
                    val offsetByte = iKeyDataTransmission.intToLittleEndianBytes(offsetSeconds)
                    val offsetSecondsBytes = offsetSeconds.toString().toCharArray().map { it.toByte() }.toByteArray()
                    val bytes = offsetByte + offsetSecondsBytes
                    Timber.d("set time zone offset $id to: $offsetSeconds, bytes: ${bytes.toHex()}, offsetSecondsBytes: ${offsetSecondsBytes.toHex()}, size ${offsetSecondsBytes.size}")
                    statefulConnection
                        .sendCommandThenWaitSingleNotification(
                            iKeyDataTransmission.createCommand(
                                0xD9,
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                bytes
                            )
                        )
                        .filter { notification ->
                            iKeyDataTransmission.decrypt(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                            )?.let { decrypted ->
                                decrypted.component3().unSignedInt() == 0xD9
                            } ?: false
                        }
                        .take(1)
                        .singleOrError()
                        .flatMap { notification ->
                            Single.just(
                                iKeyDataTransmission.resolveD9(
                                    Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                    notification
                                )
                            )
                        }
                }
        } ?: throw NotConnectedException()
    }

    fun getTime(): Single<Int> {
        return statefulConnection.macAddress?.let { mac ->
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    statefulConnection
                        .sendCommandThenWaitSingleNotification(
                            iKeyDataTransmission.createCommand(
                                0xD2,
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT)
                            )
                        )
                        .filter { notification ->
                            iKeyDataTransmission.decrypt(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                            )?.let { decrypted ->
                                decrypted.component3().unSignedInt() == 0xD2
                            } ?: false
                        }
                        .take(1)
                        .singleOrError()
                        .flatMap { notification ->
                            Single.just(
                                iKeyDataTransmission.resolveD2(
                                    Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                    notification
                                )
                            )
                        }
                }
        } ?: throw NotConnectedException()
    }
}
