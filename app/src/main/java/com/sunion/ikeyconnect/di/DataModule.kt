package com.sunion.ikeyconnect.di

import android.content.Context
import com.sunion.ikeyconnect.LockEventLogRepositoryImpl
import com.sunion.ikeyconnect.LockInformationRepositoryImpl
import com.sunion.ikeyconnect.UserCodeRepositoryImpl
import com.sunion.ikeyconnect.data.*
import com.sunion.ikeyconnect.domain.Interface.LockEventLogRepository
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.Interface.UserCodeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [DataModule.Bind::class])
object DataModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext appContext: Context): DeviceDatabase =
        DeviceDatabase.get(appContext)

    @Provides
    @Singleton
    fun provideLockConnectionDao(database: DeviceDatabase): LockConnectionInformationDao = database.lockConnectionInformationDao()

    @Provides
    @Singleton
    fun provideUserCodeDao(database: DeviceDatabase): UserCodeDao = database.userCodeDao()

    @Provides
    @Singleton
    fun provideLockEventLogDao(database: DeviceDatabase): LockEventLogDao = database.lockEventLogDao()

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class Bind {
        @Binds
        abstract fun bindPreferenceStore(preferenceStorage: PreferenceStorage): PreferenceStore

        @Binds
        abstract fun bindLockConnectionRepository(
            lockInformationRepositoryImpl: LockInformationRepositoryImpl
        ): LockInformationRepository

        @Binds
        abstract fun bindUserCodeRepository(
            userCodeRepositoryImpl: UserCodeRepositoryImpl
        ): UserCodeRepository

        @Binds
        abstract fun bindLogRepository(
            lockEventLogRepositoryImpl: LockEventLogRepositoryImpl
        ): LockEventLogRepository
    }
}