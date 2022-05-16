package com.sunion.jetpacklock

import android.app.Application
import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.google.gson.GsonBuilder
import com.sunion.jetpacklock.api.AccountAPI
import com.sunion.jetpacklock.data.PreferenceStorage
import com.sunion.jetpacklock.data.PreferenceStore
import com.sunion.jetpacklock.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [AppModule.Bind::class])
object AppModule {
    @Provides
    @Singleton
    @ExperimentalCoroutinesApi
    fun provideAWSMobileClient(application: Application): AWSMobileClient =
        AWSMobileClient.getInstance().apply {
            initialize(application, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails?) {
                    Log.d("TAG", "AWSMobileClient: "+result?.userState)
                }

                override fun onError(e: Exception?) {
                    Log.e("TAG", e.toString())
                }
            })
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(authRepository: AuthRepository): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    @Provides
    @Singleton
    fun provideIkeyApi(client: OkHttpClient): AccountAPI = Retrofit.Builder()
        .baseUrl(BuildConfig.API_GATEWAY_ENDPOINT)
        .client(client)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder()
            .setLenient()
            .disableHtmlEscaping()
            .create()))
        .build()
        .create(AccountAPI::class.java)

    @Provides
    @Singleton
    fun provideCognitoRepository(awsMobileClient: AWSMobileClient): AuthRepository =
        CognitoAuthRepository(awsMobileClient, Dispatchers.IO)

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class Bind {
//        @Binds
//        abstract fun bindCognitoRepository(authRepository: CognitoAuthRepository): AuthRepository
        @Binds
        abstract fun bindPreferenceStore(preferenceStorage: PreferenceStorage): PreferenceStore
    }
}