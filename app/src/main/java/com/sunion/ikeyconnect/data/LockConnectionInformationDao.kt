package com.sunion.ikeyconnect.data

import androidx.room.*
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.model.LockWithUserCode
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface LockConnectionInformationDao {

    @Query("SELECT COUNT(*) FROM lock_connection_information")
    fun count(): Int

    @Query("SELECT MIN(display_index) FROM lock_connection_information LIMIT 1")
    fun minIndexOf(): Single<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLockConnectionInformation(information: LockConnectionInformation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLockConnectionInformationCompletable(information: LockConnectionInformation): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllLockConnectionCompletable(all: List<LockConnectionInformation>): Completable

    @Query("SELECT * FROM lock_connection_information ORDER BY created_at ASC")
    fun getAllLockConnectionInformation(): Single<List<LockConnectionInformation>>

    @Query("SELECT * FROM lock_connection_information WHERE macAddress = :macAddress")
    fun getLockConnectionInformation(macAddress: String): Single<LockConnectionInformation>

    @Delete
    fun deleteLockConnectionInformation(information: LockConnectionInformation): Completable

    @Transaction
    @Query("SELECT * FROM lock_connection_information WHERE macAddress = :macAddress")
    fun getLockWithUserCode(macAddress: String): Single<LockWithUserCode>

    @Query("DELETE FROM lock_connection_information")
    fun deleteAll()

    @Query("UPDATE lock_connection_information SET thing_name = :thingName WHERE macAddress = :macAddress")
    fun setThingName(macAddress: String, thingName: String)
}
