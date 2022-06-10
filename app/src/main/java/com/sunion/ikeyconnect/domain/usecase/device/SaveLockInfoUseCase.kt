package com.sunion.ikeyconnect.domain.usecase.device

import android.util.Base64
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.exception.LockAlreadyExistedException
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.model.LockInfo
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject


class SaveLockInfoUseCase @Inject constructor(
    private val lockInformationRepository: LockInformationRepository
    ) {

    @Throws(LockAlreadyExistedException::class)
    suspend operator fun invoke(lockInfo: LockInfo): LockConnectionInformation {
        var information = LockConnectionInformation(
            macAddress = lockInfo.macAddress,
            model = lockInfo.model,
            displayName = lockInfo.lockName,
            keyOne = Base64.encodeToString(hexToBytes(lockInfo.keyOne), Base64.DEFAULT),
            keyTwo = "",
            oneTimeToken = Base64.encodeToString(hexToBytes(lockInfo.oneTimeToken), Base64.DEFAULT),
            permanentToken = "",
            isOwnerToken = lockInfo.isOwnerToken,
            tokenName = "T",
            sharedFrom = lockInfo.isFrom,
            index = 0
        )

        if (runCatching {
                lockInformationRepository.get(information.macAddress)
                    .toObservable().asFlow().single()
            }.getOrNull() != null
        )
            throw LockAlreadyExistedException()

        val displayIndex =
            runCatching {
                lockInformationRepository.getMinIndexOf().toObservable().asFlow().single()
            }.getOrNull()
                ?.run { this - 2 } ?: 0
        information = information.copy(index = displayIndex)

        lockInformationRepository.save(information)

        return information
    }

    private fun hexToBytes(hexString: String): ByteArray {
        val hex: CharArray = hexString.toCharArray()
        val length = hex.size / 2
        val rawData = ByteArray(length)
        for (i in 0 until length) {
            val high = Character.digit(hex[i * 2], 16)
            val low = Character.digit(hex[i * 2 + 1], 16)
            var value = high shl 4 or low
            if (value > 127) value -= 256
            rawData[i] = value.toByte()
        }
        return rawData
    }
}