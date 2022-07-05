package com.sunion.ikeyconnect.domain.usecase.device

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import javax.inject.Inject

class IsBlueToothEnabledUseCase @Inject constructor(private val application: Application) {
    operator fun invoke(): Boolean {
        val bluetoothAdapter =
            if (!application.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                null
            } else {
                val bluetoothManager =
                    application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothManager.adapter
            }

        return bluetoothAdapter?.isEnabled ?: false
    }
}