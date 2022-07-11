package com.sunion.ikeyconnect.api

import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorInterceptor @Inject constructor(private val toastHttpException: ToastHttpException) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val response = chain.proceed(request)
        val bodyString = response.body!!.string()
        val apiName = response.request.url.toString().replace("https://api.ikey-lock.com/v1/", "")
        toastHttpException.apiName = apiName

        return response.newBuilder()
            .body(bodyString.toResponseBody(response.body?.contentType()))
            .build()
    }
}