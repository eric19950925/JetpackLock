package com.sunion.ikeyconnect.domain.model

data class UpdateTokenResponse(
    val isSuccessful: Boolean,
    val tokenIndexInDevice: Int,
    val isPermanent: Boolean,
    val permission: String,
    val token: String,
    val name: String
)
