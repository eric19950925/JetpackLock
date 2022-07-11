package com.sunion.ikeyconnect

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sunion.ikeyconnect.domain.model.LockStatus
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

inline fun unless(condition: Boolean, crossinline block: () -> Unit) {
    if (condition) {
        block.invoke()
    }
}

fun JSONObject.optNullableString(name: String, fallback: String? = null) : String? {
    return if (this.has(name) && !this.isNull(name)) {
        this.getString(name)
    } else {
        fallback
    }
}

fun String.getLockState(): Int {
    return if(this == "lock") LockStatus.LOCKED
    else if(this == "unlock") LockStatus.UNLOCKED
    else 100
}

@Composable
fun Long.isTodayOrYesterdayOrElse(): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd")
    val dateString = formatter.format(Date(this))
    return if (DateUtils.isToday(this)) stringResource(id = R.string.log_today)
    else if (DateUtils.isToday(this + DateUtils.DAY_IN_MILLIS)) stringResource(id = R.string.log_yesterday)
    else dateString
}