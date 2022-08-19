package com.sunion.ikeyconnect.domain.command

import android.util.Base64
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import timber.log.Timber

class GetEventQuantityCommand(private val iKeyDataTransmission: BleCmdRepository) :
    IKeyCommand<Unit, Int> {
    override val function: Int = 0xE0

    override fun create(key: String, data: Unit): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT)
        )
    }

    override fun parseResult(key: String, data: ByteArray): Int {
        return resolveE0(
            Base64.decode(key, Base64.DEFAULT),
            data
        )
    }
    /**
     * Resolve [E0] The quantity of log.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveE0(aesKeyTwo: ByteArray, notification: ByteArray): Int {
        return iKeyDataTransmission.decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xE0) {
                val quantity =
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).first()
                        .unSignedInt()
                Timber.d("quantity: $quantity")
                return quantity
            } else {
                throw IllegalArgumentException("Return function byte is not [E0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }


    override fun match(key: String, data: ByteArray): Boolean {
        return iKeyDataTransmission.decrypt(
            Base64.decode(key, Base64.DEFAULT), data
        )?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == 0xE0
        } ?: false
    }

}