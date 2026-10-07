package com.chiranth7.regibook.util.log

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chiranth7.regibook.RegisterApplication
import com.chiranth7.regibook.features.lic.sync.LicSyncManager

class SyncRetryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RegisterApplication
        if (app == null) return Result.success()

        // 1. Flush any queued offline error logs to Discord
        try {
            AppLogManager.flushPendingLogsToDiscord(applicationContext)
        } catch (_: Exception) {}

        AppLogManager.log(applicationContext, "SyncRetry", "Internet connection detected. Running automatic retry...")

        // 2. Retry policy sync
        return try {
            val result = LicSyncManager.syncPolicies(applicationContext, app.settingsManager)
            when (result) {
                is LicSyncManager.SyncResult.Success -> {
                    AppLogManager.log(
                        applicationContext,
                        "SyncRetry",
                        "Automatic sync succeeded upon reconnect (${result.count} policies synced)"
                    )
                    Result.success()
                }
                is LicSyncManager.SyncResult.Error -> {
                    AppLogManager.log(
                        applicationContext,
                        "SyncRetry",
                        "Sync retry failed: ${result.message}",
                        isError = true
                    )
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            AppLogManager.log(
                applicationContext,
                "SyncRetry",
                "Error during sync retry: ${e.message}",
                isError = true
            )
            Result.retry()
        }
    }
}
