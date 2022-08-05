package com.sunion.ikeyconnect.domain.model

data class EventLog(
    val eventTimeStamp: Long,
    val event: Int,
    val name: String
)
