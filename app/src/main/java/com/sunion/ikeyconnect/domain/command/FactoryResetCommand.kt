package com.sunion.ikeyconnect.domain.command

import android.util.Base64
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.LockStatusException

class FactoryResetCommand(private val iKeyDataTransmission: BleCmdRepository) :
    IKeyCommand<Unit, Boolean> {
    override val function: Int = 0xCE

    override fun create(key: String, data: Unit): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT),
        )
    }

    fun create2(key: String, adminCode: String): ByteArray {
        val admin_code = iKeyDataTransmission.stringCodeToHex(adminCode)
        val sendBytes = byteArrayOf(admin_code.size.toByte()) + admin_code
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT),
            sendBytes
        )
    }

    override fun parseResult(key: String, data: ByteArray): Boolean {
        return resolveCE(
            Base64.decode(key, Base64.DEFAULT),
            data
        )
    }
    /**
     * Resolve [CE] token generated from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    private fun resolveCE(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return iKeyDataTransmission.decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xCE) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }


    override fun match(key: String, data: ByteArray): Boolean {
        return iKeyDataTransmission.decrypt(
            Base64.decode(key, Base64.DEFAULT), data
        )?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == 0xCE
        } ?: false
    }

}