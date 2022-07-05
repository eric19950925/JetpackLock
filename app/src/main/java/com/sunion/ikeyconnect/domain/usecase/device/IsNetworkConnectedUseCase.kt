package com.sunion.ikeyconnect.domain.usecase.device

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsNetworkConnectedUseCase @Inject constructor(private val application: Application) {
    @SuppressLint("MissingPermission")
    operator fun invoke(): Boolean {
        val connManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                ?: return false
        val network: Network = connManager.activeNetwork ?: return false
        val capabilities = connManager.getNetworkCapabilities(network)
        return capabilities != null
                && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }
}