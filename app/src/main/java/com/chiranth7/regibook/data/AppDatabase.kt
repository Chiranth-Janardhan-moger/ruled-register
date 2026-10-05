package com.chiranth7.regibook.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pigmiDao(): PigmiDao
    abstract fun licDao(): LicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "register_book_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            try {
                                db.beginTransaction()
                                try {
                                    val stmt = db.compileStatement(
                                        "INSERT INTO pigmi_accounts (srNo, name, phoneNumber, address, accountNumber, dailyAmount) VALUES (?, ?, ?, ?, ?, ?)"
                                    )
                                    val names = listOf(
                                        "Rahul", "Anil", "Ramesh", "Pooja", "Suresh",
                                        "Kiran", "Vijay", "Deepa", "Manjunath", "Ganesh",
                                        "Lakshmi", "Venkatesh", "Sunita", "Prashanth", "Geetha"
                                    )
                                    val localities = listOf(
                                        "MG Road", "Jayanagar", "Malleswaram", "Indiranagar",
                                        "Rajajinagar", "Basavanagudi", "BTM Layout", "Hebbal",
                                        "Koramangala", "Whitefield"
                                    )
                                    for (i in 1..1100) {
                                        val name = "${names[i % names.size]} ${('A' + (i % 26))}"
                                        val phone = "+91 98${"%08d".format(10000000 + i)}"
                                        val addr = "${localities[i % localities.size]}, Bengaluru"
                                        val accNo = "PG-${1000 + i}"
                                        val amt = "₹${100 + (i % 10) * 50}"

                                        stmt.bindLong(1, i.toLong())
                                        stmt.bindString(2, name)
                                        stmt.bindString(3, phone)
                                        stmt.bindString(4, addr)
                                        stmt.bindString(5, accNo)
                                        stmt.bindString(6, amt)
                                        stmt.executeInsert()
                                        stmt.clearBindings()
                                    }

                                    // Pre-populate sample records for LIC cards
                                    val licStmt = db.compileStatement(
                                        "INSERT INTO lic_accounts (name, policyNumber, policyName, totalYears, lastPaymentDate, nextPaymentDate, phoneNumber, address, premiumAmount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    )
                                    val sampleLics = listOf(
                                        listOf("Kiran Kumar", "POL-8923410", "Jeevan Labh", "16 Years", "15/09/2026", "15/10/2026", "+91 99001 23456", "Indiranagar, Bengaluru", "₹2,500"),
                                        listOf("Pooja Sharma", "POL-4521908", "Jeevan Anand", "20 Years", "02/10/2026", "02/11/2026", "+91 98800 11223", "Rajajinagar, Bengaluru", "₹5,000"),
                                        listOf("Suresh Gowda", "POL-6638192", "Jeevan Umang", "25 Years", "28/08/2026", "28/11/2026", "+91 94480 33445", "Jayanagar, Bengaluru", "₹3,200")
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
