package com.sunion.ikeyconnect.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "user_code",
    indices = [Index(value = ["index"], unique = true)]
)
data class UserCode(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    val macAddress: String,

    @ColumnInfo(name = "index")
    val index: Int,

    @ColumnInfo(name = "isEnable")
    val isEnable: Boolean,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "scheduleType")
    val scheduleType: String,

    @ColumnInfo(name = "week_days")
    val weekDays: Int? = null,

    @ColumnInfo(name = "from_time")
    val from: Int? = null,

    @ColumnInfo(name = "to_time")
    val to: Int? = null,

    @ColumnInfo(name = "schedule_from")
    val scheduleFrom: Int? = null,

    @ColumnInfo(name = "schedule_to")
    val scheduleTo: Int? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = Instant.now().toEpochMilli()
)