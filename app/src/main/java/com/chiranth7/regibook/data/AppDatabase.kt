package com.chiranth7.regibook.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.data.LicDao
import com.chiranth7.regibook.features.pigmi.data.PigmiAccount
import com.chiranth7.regibook.features.pigmi.data.PigmiDao

@Database(
    entities = [
        PigmiAccount::class,
        LicAccount::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pigmiDao(): PigmiDao
    abstract fun licDao(): LicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lic_accounts ADD COLUMN commencementDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE lic_accounts ADD COLUMN lastPremiumDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE lic_accounts ADD COLUMN maturityDate TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "register_book_database"
                )
                    .addMigrations(MIGRATION_8_9)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            try {
                                db.beginTransaction()
                                try {
                                    // Pre-populate sample records for LIC cards
                                    val licStmt = db.compileStatement(
                                        "INSERT INTO lic_accounts (name, policyNumber, policyName, totalYears, lastPaymentDate, nextPaymentDate, phoneNumber, address, premiumAmount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    )
                                    val sampleLics = listOf(
                                        listOf("Chiranth Janardhan Moger", "739558210", "736 - LIC'S JEEVAN LABH PLAN", "21/15", "29/06/2025", "28/06/2026", "", "₹2,00,000", "₹11,873/Year")
                                    )
                                    for (lic in sampleLics) {
                                        for (idx in 1..9) {
                                            licStmt.bindString(idx, lic[idx - 1])
                                        }
                                        licStmt.executeInsert()
                                        licStmt.clearBindings()
                                    }

                                    db.setTransactionSuccessful()
                                } finally {
                                    db.endTransaction()
                                }
                            } catch (e: Exception) {
                                Log.e("AppDatabase", "Error populating initial data", e)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
