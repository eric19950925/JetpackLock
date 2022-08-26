package com.sunion.ikeyconnect.auto_unlock

import com.google.android.gms.tasks.Task
import com.sunion.ikeyconnect.domain.model.Event
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PendingGeofenceTaskUseCase @Inject constructor(){
    private val _GeofenceTask = MutableSharedFlow<Event<Pair<Boolean, PendingGeofenceTask>>>()
    val GeofenceTask: SharedFlow<Event<Pair<Boolean, PendingGeofenceTask>>> = _GeofenceTask
    suspend fun emitGeofenceTask(
        task: Task<Void>,
        pendingGeofenceTask: PendingGeofenceTask,
        msg: String
    ){
        if(task.isSuccessful) {
            _GeofenceTask.emit(Event.success(task.isSuccessful to pendingGeofenceTask))
        }
        else {
            _GeofenceTask.emit(Event.error(msg))
        }
    }
}