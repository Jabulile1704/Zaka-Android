package com.jabulile.zaka.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The whole of Zaka's storage: one SQLite file on the device, no account and
 * no network.
 *
 * `exportSchema` is off at version 1. A schema JSON is only worth having once
 * there is a migration to validate against it, and an exported schema that
 * nobody has committed is worse than none at all. Turn it on, through the Room
 * Gradle plugin, with the first migration.
 */
@Database(
    entities = [ExpenseEntity::class, LoyaltyCardEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ZakaDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao

    abstract fun cards(): LoyaltyCardDao

    companion object {
        private const val FILE_NAME = "zaka.db"

        @Volatile
        private var instance: ZakaDatabase? = null

        fun get(context: Context): ZakaDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): ZakaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ZakaDatabase::class.java,
                FILE_NAME,
            ).build()
    }
}
