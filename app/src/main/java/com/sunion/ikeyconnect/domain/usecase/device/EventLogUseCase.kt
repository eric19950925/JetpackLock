package com.sunion.ikeyconnect.domain.usecase.device

import android.util.Base64
import androidx.room.EmptyResultSetException
import com.sunion.ikeyconnect.domain.Interface.LockEventLogRepository
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.command.GetEventCommand
import com.sunion.ikeyconnect.domain.exception.NotConnectedException
import com.sunion.ikeyconnect.domain.model.Log
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventLogUseCase @Inject constructor(
    private val iKeyDataTransmission: BleCmdRepository,
    private val statefulConnection: StatefulConnection,
    private val lockInformationRepository: LockInformationRepository,
    private val eventLogRepository: LockEventLogRepository
) {

    private val EMPTY_RESULT = Log.LockEventLog(macAddress = "", eventTimestamp = Instant.now().epochSecond, event = 999, name = "")

    companion object {
        private val VALID_EVENT = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 64, 65, 66, 80, 81, 82, 128, 129, 130, 131)

        const val VALID_STARTING_TIMESTAMP = 1609459200L
    }

    fun queryLogFromIndex(index: Int): Single<Log.LockEventLog> {
        return statefulConnection.macAddress?.let { mac ->
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    statefulConnection.sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xE1,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                            byteArrayOf(index.toByte())
                        )
                    )
                        .filter { notification ->
                            iKeyDataTransmission.decrypt(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                            )?.let { decrypted ->
                                decrypted.component3().unSignedInt() == 0xE1
                            } ?: false
                        }
                        .take(1)
                        .singleOrError()
                        .flatMap { notification ->
                            Single.just(
                                iKeyDataTransmission.resolveE1(
                                    Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                    notification
                                )
                            )
                        }
                        .map { log ->
                            Log.LockEventLog(
                                macAddress = mac,
                                eventTimestamp = log.eventTimeStamp,
                                event = log.event,
                                name = log.name
                            )
                        }
                }
        } ?: throw NotConnectedException()
    }

    fun queryLogQuantity(): Single<Int> {
        return statefulConnection.macAddress?.let { mac ->
            lockInformationRepository
                .get(mac)
                .flatMap { lockConnection ->
                    statefulConnection.sendCommandThenWaitSingleNotification(
                        iKeyDataTransmission.createCommand(
                            0xE0,
                            Base64.decode(lockConnection.keyTwo, Base64.DEFAULT)
                        )
                    )
                        .filter { notification ->
                            iKeyDataTransmission.decrypt(
                                Base64.decode(lockConnection.keyTwo, Base64.DEFAULT), notification
                            )?.let { decrypted ->
                                decrypted.component3().unSignedInt() == 0xE0
                            } ?: false
                        }
                        .take(1)
                        .singleOrError()
                        .flatMap { notification ->
                            Single.just(
                                iKeyDataTransmission.resolveE0(
                                    Base64.decode(lockConnection.keyTwo, Base64.DEFAULT),
                                    notification
                                )
                            )
                        }
                }
        } ?: throw NotConnectedException()
    }

    fun queryLogFromDevice(): Completable {
        return statefulConnection.macAddress?.let { mac ->
            eventLogRepository
                .getLatestLog(macAddress = mac)
                .onErrorReturn {
                    Timber.d(it)
                    when (it) {
                        // first time
                        is EmptyResultSetException -> EMPTY_RESULT
                        else -> throw it
                    }
                }
                .flatMapCompletable { latest ->
                    queryLogQuantity()
                        .doOnSuccess { Timber.d("log quantity from device: $it") }
                        .flatMapCompletable { quantity ->
                            Observable
                                .fromIterable((0..quantity.minus(1)).reversed())
                                .concatMap { index -> queryLogFromIndex(index).toObservable() }
                                .doOnNext { Timber.d("read log: $it") }
                                .takeUntil { fromDevice -> latest.eventTimestamp == fromDevice.eventTimestamp }
                                .filter { log -> log.event in VALID_EVENT }
                                .flatMapCompletable { log -> eventLogRepository.save(log) }
                        }
                }
        } ?: Completable.error(NotConnectedException())
    }

    fun getAllLogFromLocalCache(): Flowable<List<Log.LockEventLog>> {
        return statefulConnection.macAddress?.let { mac ->
            Timber.d("macAddress: $mac")
            eventLogRepository.getAll(mac)
        } ?: Flowable.error(NotConnectedException())
    }

    fun insertHeaderByDate(original: List<Log.LockEventLog>): List<Log> {
        val destination = mutableListOf<Log>()

        val today = Instant.now()
            .atZone(ZoneId.of(ZoneId.systemDefault().id).rules.getOffset(Instant.now()))
            .truncatedTo(ChronoUnit.DAYS)
            .toEpochSecond()

        val yesterday = Instant.now()
            .atZone(ZoneId.of(ZoneId.systemDefault().id).rules.getOffset(Instant.now()))
            .truncatedTo(ChronoUnit.DAYS)
            .minusDays(1L)
            .toEpochSecond()

        val launchTime = Instant.ofEpochSecond(1609459200)
            .epochSecond

        original.groupBy {
            Instant.ofEpochSecond(it.eventTimestamp)
                .atZone(ZoneId.of(ZoneId.systemDefault().id).rules.getOffset(Instant.now()))
                .truncatedTo(ChronoUnit.DAYS)
                .toEpochSecond()
        }.entries.forEach { entry: Map.Entry<Long, List<Log.LockEventLog>> ->
            val header = if (entry.key >= today) {
                Log.HeaderToday(entry.key)
            } else if (entry.key in (yesterday) until today) {
                Log.HeaderYesterday(entry.key)
            } else if (entry.key in (launchTime) until yesterday) {
                Log.HeaderDate(entry.key)
            } else {
                Log.HeaderUnknownTimestamp(entry.key)
            }

            Timber.d("add header: $header, and its value: ${entry.value}")
            destination.add(header)
            destination.addAll(entry.value)
        }
        return destination
    }

    fun deleteAll(macAddress: String): Completable {
        return eventLogRepository.deleteAll(macAddress)
    }
}