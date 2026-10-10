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
    version = 11,
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

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildPigmiAccountsTable(db)
            }
        }

        val MIGRATION_8_10 = object : Migration(8, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lic_accounts ADD COLUMN commencementDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE lic_accounts ADD COLUMN lastPremiumDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE lic_accounts ADD COLUMN maturityDate TEXT NOT NULL DEFAULT ''")
                rebuildPigmiAccountsTable(db)
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lic_accounts ADD COLUMN kannadaName TEXT NOT NULL DEFAULT ''")
            }
        }

        private fun rebuildPigmiAccountsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pigmi_accounts_temp` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`srNo` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`phoneNumber` TEXT NOT NULL, " +
                    "`address` TEXT NOT NULL, " +
                    "`accountNumber` TEXT NOT NULL, " +
                    "`dailyAmount` TEXT NOT NULL" +
                ")"
            )

            try {
                db.execSQL(
                    "INSERT INTO `pigmi_accounts_temp` (`id`, `srNo`, `name`, `phoneNumber`, `address`, `accountNumber`, `dailyAmount`) " +
                    "SELECT `id`, `srNo`, `name`, " +
                    "COALESCE(`phoneNumber`, ''), " +
                    "COALESCE(`address`, ''), " +
                    "COALESCE(`accountNumber`, ''), " +
                    "COALESCE(`dailyAmount`, '') " +
                    "FROM `pigmi_accounts`"
                )
            } catch (e: Exception) {
                Log.w("AppDatabase", "Fallback copy for pigmi_accounts migration", e)
                try {
                    db.execSQL(
                        "INSERT INTO `pigmi_accounts_temp` (`id`, `srNo`, `name`, `phoneNumber`, `address`, `accountNumber`, `dailyAmount`) " +
                        "SELECT `id`, `srNo`, `name`, '', '', '', '' FROM `pigmi_accounts`"
                    )
                } catch (_: Exception) {}
            }

            db.execSQL("DROP TABLE IF EXISTS `pigmi_accounts`")
            db.execSQL("ALTER TABLE `pigmi_accounts_temp` RENAME TO `pigmi_accounts`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pigmi_accounts_srNo` ON `pigmi_accounts` (`srNo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pigmi_accounts_name` ON `pigmi_accounts` (`name`)")
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "register_book_database"
            )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_8_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildAndValidateDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildAndValidateDatabase(context: Context): AppDatabase {
            val db = buildDatabase(context)
            return try {
                db.openHelper.writableDatabase
                db
            } catch (e: Exception) {
                Log.e("AppDatabase", "Database open/migration failed. Recreating clean database.", e)
                try {
                    db.close()
                } catch (_: Exception) {}
                try {
                    context.deleteDatabase("register_book_database")
                } catch (_: Exception) {}
                val freshDb = buildDatabase(context)
                freshDb.openHelper.writableDatabase
                freshDb
            }
        }
    }
}
