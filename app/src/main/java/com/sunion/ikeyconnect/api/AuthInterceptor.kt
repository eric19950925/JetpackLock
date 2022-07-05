package com.sunion.ikeyconnect.api

import com.amazonaws.mobile.client.AWSMobileClient
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class AuthInterceptor (private val awsMobileClient: AWSMobileClient) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder().apply {
            val tokenString = awsMobileClient.tokens.idToken.tokenString
            Timber.tag("AuthInterceptor").d("Authorization: $tokenString")
            addHeader("Authorization", "Bearer $tokenString")
        }
        return chain.proceed(requestBuilder.build())
    }
}