package com.sunion.ikeyconnect.domain.blelock

import android.util.Base64
import com.sunion.ikeyconnect.domain.blelock.IKeyCommand.Companion.CMD_CONNECT
import com.sunion.ikeyconnect.domain.blelock.IKeyCommand.Companion.CMD_SET_PASSWORD_PREFIX
import com.sunion.ikeyconnect.domain.blelock.IKeyCommand.Companion.CMD_SET_SSID_PREFIX
import com.sunion.ikeyconnect.domain.blelock.IKeyCommand.Companion.DATA_INDEX
import com.sunion.ikeyconnect.domain.blelock.IKeyCommand.Companion.DATA_LENGTH_INDEX
import com.sunion.ikeyconnect.domain.blelock.IKeyCommand.Companion.FUNCTION_INDEX
import com.sunion.ikeyconnect.domain.toHex
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface IKeyCommand<R> {
    val function: Int
    fun create(key: String): ByteArray
    fun parseResult(key: String, bytes: ByteArray): R
    fun match(key: String, bytes: ByteArray): Boolean
    fun setSSID(keyTwo: String, ssid: String): ByteArray
    fun setPassword(keyTwo: String, password: String): ByteArray
    fun connect(keyTwo: String): ByteArray

    companion object {
        const val FUNCTION_INDEX = 2
        const val DATA_LENGTH_INDEX = 3
        const val DATA_INDEX = 4

        val CMD_LIST_WIFI = "L"
        val CMD_SET_SSID_PREFIX = "S"
        val CMD_SET_PASSWORD_PREFIX = "P"
        val CMD_CONNECT = "C"
    }
}

@Singleton
class WifiListCommand @Inject constructor(private val iKeyDataTransmission: BleCmdRepository) :
    IKeyCommand<String> {
    override val function: Int = 0xF0

    override fun create(key: String): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT),
            data = byteArrayOf(0x4C)
        )
    }

    override fun parseResult(key: String, bytes: ByteArray): String {
        val newBytes = iKeyDataTransmission.decrypt(Base64.decode(key, Base64.DEFAULT), bytes)!!
        Timber.d("parseResult:${newBytes.toHex()}")
        val length = newBytes[DATA_LENGTH_INDEX].toInt()
        return String(newBytes.copyOfRange(DATA_INDEX, DATA_INDEX + length))
    }

    override fun match(key: String, bytes: ByteArray): Boolean {
        Timber.d("notify: ${bytes.toHex()}")
        val decrypted = iKeyDataTransmission.decrypt(Base64.decode(key, Base64.DEFAULT), bytes)!!
        val cmd = decrypted.copyOfRange(FUNCTION_INDEX, decrypted.size - 1).first()
            .unSignedInt()


        val match = cmd == function
        Timber.d("match:$match (${decrypted.toHex()})")
        return match
    }

    override fun setSSID(keyTwo: String, ssid: String): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(keyTwo, Base64.DEFAULT),
            data = ( CMD_SET_SSID_PREFIX + ssid ).toByteArray()
        )
    }

    override fun setPassword(keyTwo: String, password: String): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(keyTwo, Base64.DEFAULT),
            data = (CMD_SET_PASSWORD_PREFIX + password ).toByteArray()
        )
    }

    override fun connect(keyTwo: String): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(keyTwo, Base64.DEFAULT),
            data = CMD_CONNECT.toByteArray()
        )
    }
}