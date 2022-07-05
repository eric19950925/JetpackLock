package com.sunion.ikeyconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunion.ikeyconnect.domain.model.UserCode
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface UserCodeDao {

    @Query("SELECT COUNT(*) FROM user_code")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserCode(userCode: UserCode): Completable

    @Query("SELECT * FROM user_code ORDER BY createdAt ASC")
    fun getAllUserCode(): Single<List<UserCode>>

    @Query("SELECT * FROM user_code WHERE `index` = :index")
    fun getUserCode(index: Int): Single<UserCode>

    @Query("DELETE FROM user_code WHERE `index` = :index")
    fun deleteUserCode(index: Int): Completable

    @Query("DELETE FROM user_code WHERE `macAddress` = :macAddress")
    fun deleteUserCodes(macAddress: String): Completable
}