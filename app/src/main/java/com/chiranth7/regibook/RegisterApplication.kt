package com.chiranth7.regibook

import android.app.Application
import com.chiranth7.regibook.data.AppDatabase
import com.chiranth7.regibook.features.lic.data.LicRepository
import com.chiranth7.regibook.features.lic.worker.DailyLicReminderWorker
import com.chiranth7.regibook.features.pigmi.data.PigmiRepository
import com.chiranth7.regibook.util.SettingsManager
import com.chiranth7.regibook.util.update.UpdateManager

class RegisterApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val pigmiRepository: PigmiRepository by lazy { PigmiRepository(database.pigmiDao()) }
    val licRepository: LicRepository by lazy { LicRepository(database.licDao()) }
    val settingsManager: SettingsManager by lazy { SettingsManager(this) }
    val updateManager: UpdateManager by lazy { UpdateManager(this) }

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                com.chiranth7.regibook.util.log.AppLogManager.log(
                    this,
                    "Crash",
                    "Uncaught exception on ${thread.name}: ${throwable.stackTraceToString().take(800)}",
                    isError = true
                )
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
        DailyLicReminderWorker.schedule(this)
    }
}

