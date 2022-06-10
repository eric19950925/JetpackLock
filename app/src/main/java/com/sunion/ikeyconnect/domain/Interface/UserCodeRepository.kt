package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.UserCode
import io.reactivex.Completable
import io.reactivex.Single

interface UserCodeRepository {
    fun save(userCode: UserCode): Completable
    fun getAll(): Single<List<UserCode>>
    fun get(index: Int): Single<UserCode>
    fun delete(index: Int): Completable
    fun deleteAll(macAddress: String): Completable
}
