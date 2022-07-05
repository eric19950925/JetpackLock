package com.sunion.ikeyconnect.di

import android.content.Context
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.internal.RxBleLog
import com.sunion.ikeyconnect.domain.blelock.ReactiveStatefulConnection
import com.sunion.ikeyconnect.domain.blelock.StatefulConnection
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [BleModule.Bind::class])
object BleModule {

    @Provides
    @Singleton
    fun provideRxBleClient(@ApplicationContext context: Context): RxBleClient {
        val rxRleClient = RxBleClient.create(context)
        RxBleClient.setLogLevel(RxBleLog.DEBUG)
        return rxRleClient
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class Bind {
        @Binds
        @Singleton
        abstract fun bindStatefulConnection(reactiveStatefulConnection: ReactiveStatefulConnection): StatefulConnection
    }
}