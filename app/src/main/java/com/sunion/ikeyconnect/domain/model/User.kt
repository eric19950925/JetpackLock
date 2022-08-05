package com.sunion.ikeyconnect.domain.model

import java.io.Serializable

data class User(
    val isValid: Boolean,
    val isPermanent: Boolean,
    val indexInDevice: Int = -1,
    val isOwner: Boolean,
    val name: String,
    val permission: String,
    val token: String,
    val lockName: String = "",
    val isDeleteSelfDisabled: Boolean? = null
) : Serializable
