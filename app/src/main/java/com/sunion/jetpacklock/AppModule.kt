package com.sunion.jetpacklock

import android.app.Application
import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class Bind {
        @Binds
        abstract fun bindCognitoRepository(authRepository: CognitoAuthRepository):AuthRepository

    }
}