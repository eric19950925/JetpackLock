package com.sunion.ikeyconnect.domain.command

import android.util.Base64
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import com.sunion.ikeyconnect.domain.model.EventLog
import com.sunion.ikeyconnect.domain.toHex
import timber.log.Timber
import java.nio.ByteBuffer

class GetEventCommand(private val iKeyDataTransmission: BleCmdRepository) :
    IKeyCommand<Unit, EventLog> {
    override val function: Int = 0xE1

    override fun create(key: String, data: Unit): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT)
        )
    }

    fun create2(key: String, index: Int): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT),
            byteArrayOf(index.toByte())
        )
    }

    override fun parseResult(key: String, data: ByteArray): EventLog {
        return resolveE1(
            Base64.decode(key, Base64.DEFAULT),
            data
        )
    }
    /**
     * Resolve [E1] Get the user code at specific index.
     *
     * @param notification Data return from device.
     * @return ByteArray represent a user code setting.
     *
     * */
    fun resolveE1(aesKeyTwo: ByteArray, notification: ByteArray): EventLog {
        return aesKeyTwo.let { keyTwo ->
            iKeyDataTransmission.decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xE1) {
                    val dataLength = decrypted.component4().unSignedInt()
                    val data = decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
                    Timber.d("[E1] dataLength: $dataLength, ${data.toHex()}")

                    val timestamp =
                        Integer.reverseBytes(ByteBuffer.wrap(data.copyOfRange(0, 4)).int).toLong()
                    val event = data.component5().unSignedInt()
                    val name = data.copyOfRange(5, data.size)
                    val log = EventLog(
                        eventTimeStamp = timestamp,
                        event = event,
                        name = String(name)
                    )
                    Timber.d("[E1] read log from device: $log")
                    log
                } else {
                    throw IllegalArgumentException("Return function byte is not [E1]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }


    override fun match(key: String, data: ByteArray): Boolean {
        return iKeyDataTransmission.decrypt(
            Base64.decode(key, Base64.DEFAULT), data
        )?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == 0xE1
        } ?: false
    }

}