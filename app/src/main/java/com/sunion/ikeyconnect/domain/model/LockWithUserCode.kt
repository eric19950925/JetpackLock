package com.sunion.ikeyconnect.domain.model

import androidx.room.Embedded
import androidx.room.Relation

data class LockWithUserCode(
    @Embedded val lockConnection: LockConnectionInformation,
    @Relation(
        parentColumn = "macAddress",
        entityColumn = "macAddress"
    )
    val codes: List<UserCode>
)
