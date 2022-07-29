package com.sunion.ikeyconnect.api

import com.sunion.ikeyconnect.domain.model.sunion_service.*
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.*
import retrofit2.http.*

interface DeviceAPI {
    /**
     * 授權配對
     */
    @POST("device-provision/create")
    suspend fun deviceProvisionCreate(
        @Body request: DeviceProvisionCreateRequest
    ): DeviceProvisionCreateResponse
    /**
     * 查詢授權配對資訊
     */
    @POST("device-provision/ticket/get")
    suspend fun deviceProvisionTicketGet(
        @Body request: DeviceProvisionTicketGetRequest
    ): DeviceProvisionTicketGetResponse

    /**
     * 裝置列表
     */
    @GET("device-list")
    suspend fun deviceList(
        @Query("clienttoken") clientToken: String
    ): DeviceListResponse

    /**
     * 更新裝置註冊資訊
     */
    @POST("device-registry/update")
    suspend fun deviceRegistryUpdate(@Body request: DeviceRegistryUpdateRequest): DeviceUpdateResponse

    /**
     * 更新裝置 Access code
     */
    @POST("device-accesscode/update")
    suspend fun deviceAccessCodeUpdate(@Body request: DeviceAccessCodeUpdateRequest): DeviceUpdateResponse

    /**
     * 更新裝置 Access code
     */
    @POST("device-accesscode/get")
    suspend fun deviceAccessCodeGet(@Body request: DeviceAccessCodeGetRequest): DeviceAccessCodeGetResponse

    /**
     * 裝置刪除
     */
    @POST("device-provision/delete")
    suspend fun deviceProvisionDelete(@Body request: DeviceProvisionDeleteRequest): DeviceUpdateResponse

    /**
     * 裝置控制 - 開/關
     */
    @POST("device-shadow/update")
    suspend fun deviceShadowUpdateLock(
        @Body request: DeviceShadowUpdateLockRequest
    ): DeviceShadowUpdateLockResponse

    /**
     * 裝置控制 - 取得門向
     */
    @POST("device-shadow/update")
    suspend fun deviceShadowUpdateRunCheck(@Body request: DeviceShadowUpdateRunCheckRequest): DeviceShadowUpdateLockResponse

    /**
     * 裝置設定 - 取得事件紀錄
     */
    @POST("event/get")
    suspend fun eventGet(@Body request: EventGetRequest): EventGetResponse

    /**
     * 取得裝置註冊資訊
     */
    @POST("device-registry/get")
    suspend fun deviceRegistryGet(@Body request: RegistryGetRequest): RegistryGetResponse

}