package com.chiranth7.regibook.features.lic.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chiranth7.regibook.MainActivity
import com.chiranth7.regibook.R
import com.chiranth7.regibook.data.AppDatabase
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.features.lic.util.PaymentReminderStatus
import java.util.concurrent.TimeUnit

class DailyLicReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            try {
                val settingsManager = com.chiranth7.regibook.util.SettingsManager(context)
                com.chiranth7.regibook.features.lic.sync.LicSyncManager.syncPolicies(context, settingsManager)
            } catch (_: Exception) {}
            checkAndPostReminders(context)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_lic_reminder_worker"
        const val CHANNEL_ID = "lic_policy_reminders"
        const val NOTIFICATION_GROUP_KEY = "com.chiranth7.regibook.LIC_REMINDERS"

        /**
         * Schedules the periodic background check 3 times a day (every 8 hours).
         */
        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<DailyLicReminderWorker>(
                    8, TimeUnit.HOURS,
                    15, TimeUnit.MINUTES // flex interval
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } catch (e: Exception) {
                // Catch IllegalStateException when running in unit tests/Robolectric without WorkManager test configuration
            }
        }

        /**
         * Checks all policies and posts notifications for those due soon (15, 7, 3, 0 days or overdue).
         * Suppresses notifications if marked as notified for the current due date.
         * Can be called from the worker or directly from UI/ViewModel on refresh.
         */
        suspend fun checkAndPostReminders(context: Context) {
            val database = AppDatabase.getDatabase(context)
            val accounts = database.licDao().getAllAccountsList()
            if (accounts.isEmpty()) return

            val settingsManager = com.chiranth7.regibook.util.SettingsManager(context)
            createNotificationChannel(context)

            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                return
            }

            for (account in accounts) {
                if (account.nextPaymentDate.isBlank()) continue

                // Check if policy has already been marked as notified for this payment cycle
                if (settingsManager.isPolicyNotified(account.policyNumber, account.nextPaymentDate)) {
                    continue
                }

                val reminder = PaymentReminderHelper.calculateReminder(account.nextPaymentDate)
                val diff = reminder.daysDifference

                // Trigger on 15 days, 7 days, 3 days, due date (0), or recently overdue
                val shouldNotify = when (reminder.status) {
                    PaymentReminderStatus.DUE_TODAY -> true
                    PaymentReminderStatus.OVERDUE -> diff in 1..7 // Notify up to 7 days overdue
                    PaymentReminderStatus.DUE_SOON -> diff in listOf(15L, 7L, 3L, 2L, 1L)
                    PaymentReminderStatus.UPCOMING -> diff == 15L
                    PaymentReminderStatus.COMPLETED,
                    PaymentReminderStatus.NOT_SET -> false
                }

                if (shouldNotify) {
                    postNotification(context, account, reminder.status, reminder.formattedDueDate, reminder.statusMessage)
                }
            }
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_lic_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_lic_desc)
                    enableLights(true)
                    enableVibration(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        private fun postNotification(
            context: Context,
            account: LicAccount,
            status: PaymentReminderStatus,
            dueDateFormatted: String,
            statusMessage: String
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "lic")
                putExtra("policy_id", account.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                account.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationId = account.id.toInt() + 1000

            val markNotifiedIntent = Intent(context, com.chiranth7.regibook.features.lic.receiver.MarkNotifiedReceiver::class.java).apply {
                action = com.chiranth7.regibook.features.lic.receiver.MarkNotifiedReceiver.ACTION_MARK_NOTIFIED
                putExtra(com.chiranth7.regibook.features.lic.receiver.MarkNotifiedReceiver.EXTRA_POLICY_NUMBER, account.policyNumber)
                putExtra(com.chiranth7.regibook.features.lic.receiver.MarkNotifiedReceiver.EXTRA_DUE_DATE, account.nextPaymentDate)
                putExtra(com.chiranth7.regibook.features.lic.receiver.MarkNotifiedReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }

            val markNotifiedPendingIntent = PendingIntent.getBroadcast(
                context,
                account.id.toInt() + 5000,
                markNotifiedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = when (status) {
                PaymentReminderStatus.DUE_TODAY -> "LIC Premium Due Today: ${account.name}"
                PaymentReminderStatus.OVERDUE -> "LIC Premium Overdue: ${account.name}"
                else -> "LIC Premium Reminder: ${account.name}"
            }

            val content = buildString {
                append("Policy: ${account.policyNumber}")
                if (account.policyName.isNotBlank()) {
                    append(" • ${account.policyName}")
                }
                if (account.premiumAmount.isNotBlank()) {
                    append(" • ${account.premiumAmount}")
                }
                append(" ($statusMessage)")
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "$content\nDue Date: $dueDateFormatted\nTap to open RegiBook and contact customer."
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    context.getString(R.string.mark_as_notified),
                    markNotifiedPendingIntent
                )
                .setGroup(NOTIFICATION_GROUP_KEY)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (_: SecurityException) {
                // In case POST_NOTIFICATIONS was revoked
            }
        }
    }
}
