package com.chiranth7.regibook.features.lic.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.chiranth7.regibook.R
import com.chiranth7.regibook.util.SettingsManager

class MarkNotifiedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val policyNumber = intent.getStringExtra(EXTRA_POLICY_NUMBER) ?: return
        val dueDate = intent.getStringExtra(EXTRA_DUE_DATE) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val settingsManager = SettingsManager(context)
        settingsManager.setPolicyNotified(policyNumber, dueDate, true)

        if (notificationId != -1) {
            try {
                NotificationManagerCompat.from(context).cancel(notificationId)
            } catch (_: Exception) {}
        }

        try {
            Toast.makeText(context, context.getString(R.string.marked_as_notified), Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_MARK_NOTIFIED = "com.chiranth7.regibook.ACTION_MARK_NOTIFIED"
        const val EXTRA_POLICY_NUMBER = "extra_policy_number"
        const val EXTRA_DUE_DATE = "extra_due_date"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
