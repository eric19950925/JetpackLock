package com.sunion.ikeyconnect.auto_unlock

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class GeoFenceRegisterReceiver @Inject constructor() : DaggerBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("MY_PACKAGE_REPLACED !!!")

        if (intent.action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            Timber.d("ACTION_MY_PACKAGE_REPLACED !!!")
        }
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Timber.d("ACTION_BOOT_COMPLETED !!!")
        }
    }
}
