package com.sunion.ikeyconnect.domain.Interface

interface ILockProvider {
    suspend fun getLockByMacAddress(macAddress: String): Lock?

    suspend fun getLockByQRCode(content: String): Lock?
}