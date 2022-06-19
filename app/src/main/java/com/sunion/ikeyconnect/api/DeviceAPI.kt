package com.sunion.ikeyconnect.api

import com.sunion.ikeyconnect.domain.model.sunion_service.*
import retrofit2.http.*

interface DeviceAPI {
    /**
     * 授權配對
     */
    @Headers("Content-Type: application/json")
    @POST("device-provision/create")
    suspend fun deviceProvisionCreate(
        @Header("Authorization") idToken: String,
        @Body request: DeviceProvisionCreateRequest
    ): DeviceProvisionCreateResponse

    /**
     * 裝置列表
     */
    @Headers("Content-Type: application/json")
    @GET("device-list")
    suspend fun deviceList(
        @Header("Authorization") idToken: String,
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
     * 裝置刪除
     */
    @POST("device-provision/delete")
    suspend fun deviceProvisionDelete(@Body request: DeviceProvisionDeleteRequest): DeviceUpdateResponse

    /**
     * 裝置控制 - 開/關
     */
    @Headers("Content-Type: application/json")
    @POST("device-shadow/update")
    suspend fun deviceShadowUpdateLock(
        @Header("Authorization") idToken: String,
        @Body request: DeviceShadowUpdateLockRequest
    ): DeviceShadowUpdateLockResponse

    /**
     * 裝置控制 - 取得門向
     */
    @POST("device-shadow/update")
    suspend fun deviceShadowUpdateRunCheck(@Body request: DeviceShadowUpdateRunCheckRequest): DeviceShadowUpdateLockResponse
}