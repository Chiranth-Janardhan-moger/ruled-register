package com.chiranth7.regibook.features.lic.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class PaymentReminderStatus {
    OVERDUE,
    DUE_TODAY,
    DUE_SOON,
    UPCOMING,
    NOT_SET
}

data class PaymentReminderInfo(
    val status: PaymentReminderStatus,
    val daysDifference: Long,
    val formattedDueDate: String,
    val statusMessage: String
)

object PaymentReminderHelper {
    private val dateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    )

    fun parseDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(dateStr.trim())
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    fun calculateReminder(nextPaymentDate: String): PaymentReminderInfo {
        val dueDate = parseDate(nextPaymentDate)
        if (dueDate == null) {
            return PaymentReminderInfo(
                status = PaymentReminderStatus.NOT_SET,
                daysDifference = 0,
                formattedDueDate = nextPaymentDate,
                statusMessage = ""
            )
        }

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dueCal = Calendar.getInstance().apply {
            time = dueDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = dueCal.timeInMillis - todayCal.timeInMillis
        val daysDiff = TimeUnit.MILLISECONDS.toDays(diffMillis)

        val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(dueDate)

        return when {
            daysDiff < 0 -> {
                val overdueDays = -daysDiff
                PaymentReminderInfo(
                    status = PaymentReminderStatus.OVERDUE,
                    daysDifference = overdueDays,
                    formattedDueDate = formattedDate,
                    statusMessage = "Overdue by $overdueDays ${if (overdueDays == 1L) "day" else "days"}"
                )
            }
            daysDiff == 0L -> {
                PaymentReminderInfo(
                    status = PaymentReminderStatus.DUE_TODAY,
                    daysDifference = 0,
                    formattedDueDate = formattedDate,
                    statusMessage = "Due today!"
                )
            }
            daysDiff <= 15 -> {
                PaymentReminderInfo(
                    status = PaymentReminderStatus.DUE_SOON,
                    daysDifference = daysDiff,
                    formattedDueDate = formattedDate,
                    statusMessage = "Due in $daysDiff ${if (daysDiff == 1L) "day" else "days"}"
                )
            }
            else -> {
                PaymentReminderInfo(
                    status = PaymentReminderStatus.UPCOMING,
                    daysDifference = daysDiff,
                    formattedDueDate = formattedDate,
                    statusMessage = "Due in $daysDiff days"
                )
            }
        }
    }

    fun generateReminderMessage(
        name: String,
        policyNumber: String,
        policyName: String,
        nextPaymentDate: String,
        premiumAmount: String
    ): String {
        val policyDesc = if (policyName.isNotBlank()) "$policyName ($policyNumber)" else "No: $policyNumber"
        val amountDesc = if (premiumAmount.isNotBlank()) " of $premiumAmount" else ""
        return "Dear $name, gentle reminder that your LIC Policy premium$amountDesc for policy $policyDesc is due on $nextPaymentDate. Kindly pay before the due date to keep your policy active."
    }
}
