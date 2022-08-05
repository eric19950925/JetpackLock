package com.sunion.ikeyconnect.domain.model

data class AddUserResponse(
    val isSuccessful: Boolean,
    val tokenIndexInDevice: Int,
    val token: String,
    val content: String = "",
    val lockName: String = ""
)
