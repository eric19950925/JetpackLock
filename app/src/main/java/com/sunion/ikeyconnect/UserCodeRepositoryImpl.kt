package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.data.UserCodeDao
import com.sunion.ikeyconnect.domain.Interface.UserCodeRepository
import com.sunion.ikeyconnect.domain.model.UserCode
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCodeRepositoryImpl @Inject constructor(
    private val userCodeDao: UserCodeDao
) : UserCodeRepository {

    override fun save(userCode: UserCode): Completable {
        return userCodeDao.insertUserCode(userCode)
    }

    override fun getAll(): Single<List<UserCode>> {
        return userCodeDao.getAllUserCode()
    }

    override fun get(index: Int): Single<UserCode> {
        return userCodeDao.getUserCode(index)
    }

    override fun delete(index: Int): Completable {
        return userCodeDao.deleteUserCode(index)
    }

    override fun deleteAll(macAddress: String): Completable {
        return userCodeDao.deleteUserCodes(macAddress)
    }
}
