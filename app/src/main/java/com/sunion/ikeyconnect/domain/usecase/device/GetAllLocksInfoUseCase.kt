package com.sunion.ikeyconnect.domain.usecase.device

import com.polidea.rxandroidble2.RxBleClient
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAllLocksInfoUseCase @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val lockInformationRepository: LockInformationRepository
) {
    fun invoke(): Single<List<LockConnectionInformation>> {
        return lockInformationRepository.getAll()
//            .map { locks ->
//                locks.map { lock ->
//
//                }
//            }
    }
}