package com.sunion.jetpacklock.api

import com.sunion.jetpacklock.domain.usecase.TimeResponse
import okhttp3.RequestBody
import retrofit2.http.*

interface AccountAPI {
    @POST("user/delete")
    suspend fun deleteUser(
        @Header("Authorization") idToken: String,
//        @Body postBody: RequestBody
        )

    @POST("app-notification")
    suspend fun setFCM(
        @Header("Authorization") idToken: String,
        @Body postBody: RequestBody
    )

    @Headers("Content-Type: application/json")
    @GET("time")
    suspend fun getTime(
        @Header("Authorization") idToken: String,
        @Query("timezone") timezone: String,
        @Query("clienttoken") clienttoken: String,
    ): TimeResponse

    @Headers("Content-Type: application/json")
    @POST("acl/attach-policy")
    suspend fun attachPolicy(
        @Header("Authorization") idToken: String,
        @Body postBody: RequestBody
    )

}