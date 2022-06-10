package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.data.LockEventLogDao
import com.sunion.ikeyconnect.domain.Interface.LockEventLogRepository
import com.sunion.ikeyconnect.domain.model.Log
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockEventLogRepositoryImpl @Inject constructor(
    private val logDao: LockEventLogDao
) : LockEventLogRepository {

    override fun save(eventLog: Log.LockEventLog): Completable {
        return logDao.insertUserCode(eventLog)
    }

    override fun getLatestLog(macAddress: String): Single<Log.LockEventLog> {
        return logDao.getLatestLog(macAddress)
    }

    override fun getAll(macAddress: String): Flowable<List<Log.LockEventLog>> {
        return logDao.getAll(macAddress)
    }

    override fun deleteAll(macAddress: String): Completable {
        return logDao.deleteAll(macAddress)
    }
}
