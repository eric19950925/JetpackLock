package com.sunion.ikeyconnect.domain.command

import android.util.Base64
import com.sunion.ikeyconnect.domain.Interface.WifiListResult
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.command.IKeyCommand.Companion.CMD_CONNECT
import com.sunion.ikeyconnect.domain.command.IKeyCommand.Companion.CMD_SET_PASSWORD_PREFIX
import com.sunion.ikeyconnect.domain.command.IKeyCommand.Companion.CMD_SET_SSID_PREFIX
import com.sunion.ikeyconnect.domain.command.IKeyCommand.Companion.DATA_LENGTH_INDEX
import com.sunion.ikeyconnect.domain.command.IKeyCommand.Companion.FUNCTION_INDEX
import com.sunion.ikeyconnect.domain.toHex
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiListCommand @Inject constructor(private val iKeyDataTransmission: BleCmdRepository) :
    BaseCommand<Unit, WifiListResult>(iKeyDataTransmission)  {
    override val function: Int = 0xF0
    private val connectWifiState = mutableListOf<String>()

    override fun create(key: String, data: Unit): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT),
            data = byteArrayOf(0x4C)
        )
    }

    override fun parseResult(key: String, data: ByteArray): WifiListResult {
        val newBytes = iKeyDataTransmission.decrypt(Base64.decode(key, Base64.DEFAULT), data)!!
        val length = newBytes[DATA_LENGTH_INDEX].toInt()
        val response =
            String(newBytes.copyOfRange(IKeyCommand.DATA_INDEX, IKeyCommand.DATA_INDEX + length))
        Timber.d("response:$response")
        if (response == "LE")
            return WifiListResult.End
        return WifiListResult.Wifi(response.substring(2), response.substring(1, 2) == "Y")
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

    fun setSSID(keyTwo: String, ssid: String): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(keyTwo, Base64.DEFAULT),
            data = ( CMD_SET_SSID_PREFIX + ssid ).toByteArray()
        )
    }

    fun setPassword(keyTwo: String, password: String): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(keyTwo, Base64.DEFAULT),
            data = (CMD_SET_PASSWORD_PREFIX + password ).toByteArray()
        )
    }

    fun connect(keyTwo: String): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(keyTwo, Base64.DEFAULT),
            data = CMD_CONNECT.toByteArray()
        )
    }
}