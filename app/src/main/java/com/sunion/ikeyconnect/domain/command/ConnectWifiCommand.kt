package com.sunion.ikeyconnect.domain.command

import android.util.Base64
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.command.IKeyCommand.Companion.CMD_CONNECT
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectWifiCommand @Inject constructor(
    private val iKeyDataTransmission: BleCmdRepository,
) :
    BaseCommand<Unit, WifiConnectState>(iKeyDataTransmission) {
    override val function: Int = 0xF0
    private val connectWifiState = mutableListOf<String>()

    override fun create(key: String, data: Unit): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = 0xF0,
            key = Base64.decode(key, Base64.DEFAULT),
            data = CMD_CONNECT.toByteArray()
        )
    }

    override fun match(key: String, data: ByteArray): Boolean {
        val decrypted = iKeyDataTransmission.decrypt(Base64.decode(key, Base64.DEFAULT), data)!!
        val responseFirstChar =
            String(decrypted.copyOfRange(4, 4 + decrypted[3].unSignedInt())).first().toString()
        return super.match(key, data)
                && responseFirstChar.contentEquals(CMD_CONNECT)
    }

    override fun parseResult(key: String, data: ByteArray): WifiConnectState {
        val decrypted = iKeyDataTransmission.decrypt(Base64.decode(key, Base64.DEFAULT), data)!!
        val length = decrypted[IKeyCommand.DATA_LENGTH_INDEX].toInt()
        val dataString =
            String(decrypted.copyOfRange(IKeyCommand.DATA_INDEX, IKeyCommand.DATA_INDEX + length))
        connectWifiState.add(dataString)
        Timber.d(dataString)
        return when (dataString) {
            "CWiFi Succ" -> WifiConnectState.ConnectWifiSuccess
            "CWiFi Fail" -> WifiConnectState.ConnectWifiFail
            "CMQTT Succ" -> WifiConnectState.ConnectAwsSuccess
            "CCloud Succ" -> WifiConnectState.ConnectCloudSuccess
            else -> WifiConnectState.Failed
        }
    }
}

sealed class WifiConnectState {
    object ConnectWifiSuccess : WifiConnectState()
    object ConnectWifiFail : WifiConnectState()
    object ConnectAwsSuccess : WifiConnectState()
    object ConnectCloudSuccess : WifiConnectState()
    object Failed : WifiConnectState()
}