package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.data.LockConnectionInformationDao
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.model.LockWithUserCode
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject


class LockInformationRepositoryImpl @Inject constructor(
    private val lockConnectionInformationDao: LockConnectionInformationDao
) : LockInformationRepository {

    override fun getMinIndexOf(): Single<Int> {
        return lockConnectionInformationDao.minIndexOf()
    }

    override fun save(information: LockConnectionInformation): Long {
        return lockConnectionInformationDao.insertLockConnectionInformation(information)
    }

    override fun saveCompletable(information: LockConnectionInformation): Completable {
        return lockConnectionInformationDao.insertLockConnectionInformationCompletable(information)
    }

    override fun saveAll(all: List<LockConnectionInformation>): Completable {
        return lockConnectionInformationDao.insertAllLockConnectionCompletable(all)
    }

    override fun getAll(): Single<List<LockConnectionInformation>> {
        return lockConnectionInformationDao.getAllLockConnectionInformation()
    }

    override fun get(macAddress: String): Single<LockConnectionInformation> {
        return lockConnectionInformationDao.getLockConnectionInformation(macAddress)
    }

    override fun delete(information: LockConnectionInformation): Completable {
        return lockConnectionInformationDao.deleteLockConnectionInformation(information)
    }

    override suspend fun deleteAll() {
        TODO("Not yet implemented")
    }

    override suspend fun setThingName(macAddress: String, thingName: String) {
        lockConnectionInformationDao.setThingName(macAddress, thingName)
    }

    override fun getLockWithUserCode(macAddress: String): Single<LockWithUserCode> {
        return lockConnectionInformationDao.getLockWithUserCode(macAddress)
    }
}
