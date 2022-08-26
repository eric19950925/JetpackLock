package com.sunion.ikeyconnect.di

import android.app.Application
import android.content.Context
import android.os.PowerManager
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.regions.Regions
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sunion.ikeyconnect.*
import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.api.AccountAPI
import com.sunion.ikeyconnect.api.AuthInterceptor
import com.sunion.ikeyconnect.api.DeviceAPI
import com.sunion.ikeyconnect.api.ErrorInterceptor
import com.sunion.ikeyconnect.domain.Interface.AuthRepository
import com.sunion.ikeyconnect.domain.Interface.MqttSupPubRepository
import com.sunion.ikeyconnect.domain.Interface.RemoteDeviceRepository
import com.sunion.ikeyconnect.domain.Interface.SunionIotService
import com.sunion.ikeyconnect.domain.exception.ToastHttpException
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [AppModule.Bind::class])
object AppModule {

    @Provides
    @Singleton
    fun provideGeoClient(@ApplicationContext context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    @Provides
    @Singleton
    fun providePowerManager(@ApplicationContext context: Context):
            PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    @ExperimentalCoroutinesApi
    fun provideAWSMobileClient(application: Application): AWSMobileClient =
        AWSMobileClient.getInstance().apply {
            initialize(application, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails?) {
                    Timber.d("AWSMobileClient: "+result?.userState)
                }

                override fun onError(e: Exception?) {
                    Timber.e( e.toString())
                }
            })
        }

    @Provides
    @Singleton
    fun provideToastHttpException(context: Context) = ToastHttpException(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(awsMobileClient: AWSMobileClient, toastHttpException: ToastHttpException): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(awsMobileClient))
            .addInterceptor(ErrorInterceptor(toastHttpException))
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
    fun provideDeviceApi(client: OkHttpClient): DeviceAPI = Retrofit.Builder()
        .baseUrl(BuildConfig.API_GATEWAY_ENDPOINT)
        .client(client)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder()
            .setLenient()
            .disableHtmlEscaping()
            .create()))
        .build()
        .create(DeviceAPI::class.java)

    @Provides
    @Singleton
    fun provideAWSIotMqttManager(): AWSIotMqttManager = AWSIotMqttManager(UUID.randomUUID().toString(), BuildConfig.AWS_IOT_CORE_END_POINT)

    @Provides
    @Singleton
    fun provideCognitoCachingCredentialsProvider(application: Application): CognitoCachingCredentialsProvider =
        CognitoCachingCredentialsProvider(application, BuildConfig.COGNITO_IDENTITY_POOL_ID, Regions.US_EAST_1)

    @Provides
    @Singleton
    fun provideTopicRepository() = TopicRepositoryImpl()

    @Provides
    @Singleton
    fun provideStatefulConnection(awsIotMqttManager: AWSIotMqttManager, topicRepositoryImpl: TopicRepositoryImpl, application: Application): MqttSupPubRepository =
        MqttStatefulConnection(awsIotMqttManager, topicRepositoryImpl, application)

    @Provides
    @Singleton
    fun provideCognitoRepository(awsMobileClient: AWSMobileClient): AuthRepository =
        CognitoAuthRepository(awsMobileClient, Dispatchers.IO)

    @Provides
    @Singleton
    fun provideRemoteDeviceRepository(deviceAPI: DeviceAPI): RemoteDeviceRepository =
        RemoteDeviceRepositoryImpl(deviceAPI)

    @Provides
    @Singleton
    fun provideSunionService(remoteDeviceRepository: RemoteDeviceRepository): SunionIotService =
        SunionIotServiceImpl(remoteDeviceRepository)

    @Provides
    @Singleton
    fun provideProvisionDomain(sunionIotService: SunionIotService) =
        ProvisionDomain(sunionIotService, Dispatchers.IO)

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @InstallIn(SingletonComponent::class)

    @Module
    abstract class Bind {
//        @Binds
//        abstract fun bindCognitoRepository(authRepository: CognitoAuthRepository): AuthRepository
//        @Binds
//        abstract fun bindPreferenceStore(preferenceStorage: PreferenceStorage): PreferenceStore
        @Binds
        abstract fun bindScheduler(appSchedulers: AppSchedulers): Scheduler
    }
}