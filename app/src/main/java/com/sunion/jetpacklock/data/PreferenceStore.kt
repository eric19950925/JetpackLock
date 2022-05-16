package com.sunion.jetpacklock.data

interface PreferenceStore {
    var firstUse: Boolean
    var connectedMacAddress: String
    var lockCurrentItem: String
    var isEnterNotify: Boolean
    var isExitNotify: Boolean

    var isGuideButtonPressed: Boolean
    var isGuidePopupMenuPressed: Boolean

    fun checkIfExist(key: String): Boolean
}
