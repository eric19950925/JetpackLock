package com.sunion.ikeyconnect.add_lock

import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvisionDomain @Inject constructor(
    private val iotService: SunionIotService,
    private val dispatcher: CoroutineDispatcher
) {
    // store ticket
    private var provisionTicket = ""
    var provisionThingName = ""

    //provision-create
    suspend fun provisionCreate(
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ){
        flow { emit(iotService.deviceProvisionCreate(
            serialNumber,
            deviceName,
            timeZone,
            timeZoneOffset,
            clientToken,
            model
        )) }.flowOn(dispatcher)
            .onEach {
                provisionTicket = it
                Timber.d("provisionTicket = $it")
            }.single()
    }
    //provision-ticket/get
    suspend fun getInfo(
        clientToken: String
    ) = flow { emit(iotService.deviceProvisionTicketGet(provisionTicket, clientToken)) }
            .flowOn(dispatcher)
            .onEach {
                Timber.d(it.toString())
                Timber.d("provisionThingName = $provisionThingName")
            }
            .map { it.ticket == provisionTicket }
            .single()


    //provision-delete
    suspend fun delete(
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ): String {
        return iotService.deviceProvisionCreate(
            serialNumber,
            deviceName,
            timeZone,
            timeZoneOffset,
            clientToken,
            model
        )
    }
    //device-registry/update
    fun updateRegistry(){
    }

    //device-accesscode/update
    fun UpdateAccessCode(){

    }
}