package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.model.LockWithUserCode
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @ About the work of using DB
 **/
interface LockInformationRepository {
    fun getMinIndexOf(): Single<Int>
    fun save(information: LockConnectionInformation): Long
    fun saveCompletable(information: LockConnectionInformation): Completable
    fun saveAll(all: List<LockConnectionInformation>): Completable
    fun getAll(): Single<List<LockConnectionInformation>>
    fun get(macAddress: String): Single<LockConnectionInformation>
    fun delete(information: LockConnectionInformation): Completable
    fun getLockWithUserCode(macAddress: String): Single<LockWithUserCode>
    suspend fun deleteAll()
    suspend fun setThingName(macAddress: String, thingName: String)
}