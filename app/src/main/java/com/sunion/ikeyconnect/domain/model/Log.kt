package com.sunion.ikeyconnect.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity

sealed class Log {

    abstract val eventTimestamp: Long

    @Entity(tableName = "event_log", primaryKeys = ["macAddress", "timestamp", "event"])
    data class LockEventLog(
        @ColumnInfo(name = "macAddress")
        val macAddress: String,

        @ColumnInfo(name = "timestamp")
        override val eventTimestamp: Long,

        @ColumnInfo(name = "event")
        val event: Int,

        @ColumnInfo(name = "name")
        val name: String
    ) : Log()

    data class HeaderToday(override val eventTimestamp: Long) : Log()
    data class HeaderYesterday(override val eventTimestamp: Long) : Log()
    data class HeaderDate(override val eventTimestamp: Long) : Log()
    data class HeaderUnknownTimestamp(override val eventTimestamp: Long) : Log()
}