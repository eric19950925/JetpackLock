package com.sunion.ikeyconnect.domain.blelock

import com.polidea.rxandroidble2.RxBleConnection

enum class BluetoothConnectState {
    CONNECTING, CONNECTED, DISCONNECTED, DISCONNECTING
}

object BluetoothConnectStateMapper {
    fun map(state: RxBleConnection.RxBleConnectionState): BluetoothConnectState =
        when (state) {
            RxBleConnection.RxBleConnectionState.CONNECTING -> BluetoothConnectState.CONNECTING
            RxBleConnection.RxBleConnectionState.CONNECTED -> BluetoothConnectState.CONNECTED
            RxBleConnection.RxBleConnectionState.DISCONNECTED -> BluetoothConnectState.DISCONNECTED
            RxBleConnection.RxBleConnectionState.DISCONNECTING -> BluetoothConnectState.DISCONNECTING
        }
}