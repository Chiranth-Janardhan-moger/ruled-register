package com.chiranth7.regibook.util.log

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chiranth7.regibook.RegisterApplication

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

        // 2. Retry policy sync with Firestore
        return try {
            val count = com.chiranth7.regibook.data.firebase.FirestoreSyncManager.syncPoliciesFromCloud(applicationContext)
            AppLogManager.log(
                applicationContext,
                "SyncRetry",
                "Automatic sync succeeded upon reconnect ($count policies synced)"
            )
            Result.success()
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
