package com.sunion.ikeyconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunion.ikeyconnect.domain.model.Log
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface LockEventLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserCode(log: Log.LockEventLog): Completable

    @Query("SELECT * FROM event_log WHERE `macAddress` = :macAddress ORDER BY timeStamp DESC LIMIT 1")
    fun getLatestLog(macAddress: String): Single<Log.LockEventLog>

    @Query("SELECT * FROM event_log WHERE `macAddress` = :macAddress ORDER BY timeStamp DESC")
    fun getAll(macAddress: String): Flowable<List<Log.LockEventLog>>

    @Query("DELETE FROM event_log WHERE `macAddress` = :macAddress")
    fun deleteAll(macAddress: String): Completable
}