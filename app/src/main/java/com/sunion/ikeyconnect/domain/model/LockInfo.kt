package com.sunion.ikeyconnect.domain.model

data class LockInfo(
    val macAddress: String,
    val oneTimeToken: String,
    val keyOne: String,
    var keyTwo: String,
    val isOwnerToken: Boolean,
    val isFrom: String,
    val lockName: String,
    val model: String,
    val serialNumber: String? = null
) {
    val deviceName: String
        get() = "BT_Lock_" + macAddress.replace(":", "").takeLast(6)

    companion object {
        fun from(content: QRCodeContent): LockInfo = LockInfo(
            macAddress = content.a,
            oneTimeToken = content.t,
            keyOne = content.k,
            keyTwo = "",
            isOwnerToken = content.f == null,
            isFrom = content.f ?: "",
            lockName = content.l ?: "New_Lock",
            model = content.m,
            serialNumber = content.s
        )

        fun from(bleLock: BleLock): LockInfo = LockInfo(
            macAddress = bleLock.MACAddress,
            oneTimeToken = bleLock.OneTimeToken,
            keyOne = bleLock.ConnectionKey,
            keyTwo = "",
            isOwnerToken = false,
            isFrom = bleLock.SharedFrom,
            lockName = bleLock.DisplayName,
            model = "",
            serialNumber = ""
        )

        fun from(lockConnectionInfo: LockConnectionInformation): LockInfo = LockInfo(
            macAddress = lockConnectionInfo.macAddress,
            oneTimeToken = lockConnectionInfo.oneTimeToken,
            keyOne = lockConnectionInfo.keyOne,
            keyTwo = lockConnectionInfo.keyTwo,
            isOwnerToken = lockConnectionInfo.isOwnerToken,
            isFrom = lockConnectionInfo.sharedFrom ?: "",
            lockName = lockConnectionInfo.displayName,
            model = lockConnectionInfo.model
        )
    }
}