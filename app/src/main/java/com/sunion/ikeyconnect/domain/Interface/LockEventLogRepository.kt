package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.Log
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface LockEventLogRepository {
    fun save(eventLog: Log.LockEventLog): Completable
    fun getLatestLog(macAddress: String): Single<Log.LockEventLog>
    fun getAll(macAddress: String): Flowable<List<Log.LockEventLog>>
    fun deleteAll(macAddress: String): Completable
}
