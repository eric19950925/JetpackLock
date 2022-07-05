package com.sunion.ikeyconnect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunion.ikeyconnect.BuildConfig
import com.sunion.ikeyconnect.domain.model.LockConnectionInformation
import com.sunion.ikeyconnect.domain.model.Log
import com.sunion.ikeyconnect.domain.model.UserCode

@Database(
    entities = [LockConnectionInformation::class, UserCode::class, Log.LockEventLog::class],
    version = 1,
    autoMigrations = []
)
abstract class DeviceDatabase : RoomDatabase() {

    abstract fun lockConnectionInformationDao(): LockConnectionInformationDao
    abstract fun userCodeDao(): UserCodeDao
    abstract fun lockEventLogDao(): LockEventLogDao

    companion object {
        private var instance: DeviceDatabase? = null
        private const val SEPARATOR = ","
//        private val gson = Gson()

        @Synchronized
        fun get(context: Context): DeviceDatabase {
            if (instance == null) {

                instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeviceDatabase::class.java, "Room.db"
                )
//                    .addMigrations(MIGRATION_1_2)
                    .run {
                        if (BuildConfig.DEBUG) fallbackToDestructiveMigration()
                        else this
                    }
                    .build()
            }
            return instance!!
        }
    }

    /**
     * Inserts the default data fields settings if it is currently empty.
     */
    suspend fun populateInitialDataFields() {
        if (lockConnectionInformationDao().count() == 1) {
            runInTransaction {
                getInitialData().forEach {
                    lockConnectionInformationDao().insertLockConnectionInformation(it)
                }
            }
        }
    }

    private fun getInitialData() = listOf(
        LockConnectionInformation(
            macAddress = "58:8E:81:A5:61:68",
            model = "",
            displayName = "LOCK__",
            keyOne = "",
            keyTwo = "",
            oneTimeToken = "",
            permanentToken = "",
            isOwnerToken = true,
            tokenName = "Token",
            createdAt = 1614298596650,
            permission = "A",
            index = 0
        ),
        LockConnectionInformation(
            macAddress = "58:8E:81:A5:61:74",
            model = "",
            displayName = "LOCK_",
            keyOne = "",
            keyTwo = "",
            oneTimeToken = "",
            permanentToken = "",
            isOwnerToken = true,
            tokenName = "Token",
            createdAt = 1614298596653,
            permission = "A",
            index = -2
        ),
        LockConnectionInformation(
            macAddress = "32:8E:81:A5:61:74",
            model = "",
            displayName = "LOCK___",
            keyOne = "",
            keyTwo = "",
            oneTimeToken = "",
            permanentToken = "",
            isOwnerToken = true,
            tokenName = "Token",
            createdAt = 1614298596654,
            permission = "A",
            index = -4
        ),
        LockConnectionInformation(
            macAddress = "58:8E:81:AA:61:74",
            model = "",
            displayName = "LOCK____",
            keyOne = "",
            keyTwo = "",
            oneTimeToken = "",
            permanentToken = "",
            isOwnerToken = true,
            tokenName = "Token",
            createdAt = 1614298596655,
            permission = "A",
            index = -6
        )
    )
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE lockconnectioninformation ADD COLUMN thing_name TEXT")
    }
}