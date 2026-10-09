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
    COMPLETED,
    NOT_SET
}

data class PaymentReminderInfo(
    val status: PaymentReminderStatus,
    val daysDifference: Long,
    val formattedDueDate: String,
    val statusMessage: String
)

data class NextPaymentAdvanceResult(
    val newNextPaymentDate: String,
    val newLastPaymentDate: String,
    val isMatured: Boolean
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
        val trimmed = nextPaymentDate.trim()
        if (trimmed.equals("Completed", ignoreCase = true) ||
            trimmed.equals("Matured", ignoreCase = true) ||
            trimmed.contains("Completed", ignoreCase = true) ||
            trimmed.contains("Matured", ignoreCase = true)
        ) {
            return PaymentReminderInfo(
                status = PaymentReminderStatus.COMPLETED,
                daysDifference = 0,
                formattedDueDate = "Policy Matured",
                statusMessage = "Completed"
            )
        }

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

    fun parseTermAndPpt(totalYears: String): Pair<Int, Int> {
        val clean = totalYears.trim()
        if (clean.isBlank()) return Pair(0, 0)
        return if (clean.contains("/")) {
            val parts = clean.split("/")
            val term = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val ppt = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            Pair(term, ppt)
        } else {
            val num = clean.filter { it.isDigit() }.toIntOrNull() ?: 0
            if (num >= 1900) {
                Pair(0, 0)
            } else {
                Pair(num, num)
            }
        }
    }

    data class PolicyScheduleInfo(
        val termYears: Int,
        val pptYears: Int,
        val commencementDate: String,
        val endOfPremiumTermDate: String,
        val maturityDate: String,
        val remainingPaymentsCount: Int,
        val isFullyPaid: Boolean
    )

    fun calculatePolicySchedule(
        totalYears: String,
        nextPaymentDate: String,
        lastPaymentDate: String = "",
        storedCommencement: String = "",
        storedLastPremium: String = "",
        storedMaturity: String = ""
    ): PolicyScheduleInfo? {
        val (term, ppt) = parseTermAndPpt(totalYears)
        if (term <= 0 && ppt <= 0 && storedLastPremium.isBlank() && storedMaturity.isBlank()) {
            return null
        }

        val isCompleted = nextPaymentDate.trim().equals("Completed", ignoreCase = true) ||
                nextPaymentDate.trim().contains("Matured", ignoreCase = true)

        val commDate = when {
            storedCommencement.isNotBlank() -> storedCommencement.trim()
            lastPaymentDate.isNotBlank() -> {
                val p = parseDate(lastPaymentDate)
                if (p != null) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(p) else ""
            }
            nextPaymentDate.isNotBlank() && !isCompleted -> {
                val p = parseDate(nextPaymentDate)
                if (p != null) {
                    val cal = Calendar.getInstance().apply { time = p; add(Calendar.YEAR, -1) }
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                } else ""
            }
            else -> ""
        }

        val commCal = if (commDate.isNotBlank()) {
            parseDate(commDate)?.let { Calendar.getInstance().apply { time = it } }
        } else null

        val commYear = commCal?.get(Calendar.YEAR) ?: 0
        val commDayMonthStr = if (commCal != null) {
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(commCal.time)
        } else {
            val nextP = parseDate(nextPaymentDate)
            if (nextP != null) SimpleDateFormat("dd/MM", Locale.getDefault()).format(nextP) else "01/01"
        }

        val endOfPpt = when {
            storedLastPremium.isNotBlank() -> storedLastPremium.trim()
            ppt > 0 && commYear > 0 -> "$commDayMonthStr/${commYear + ppt}"
            else -> ""
        }

        val matDate = when {
            storedMaturity.isNotBlank() -> storedMaturity.trim()
            term > 0 && commYear > 0 -> "$commDayMonthStr/${commYear + term}"
            else -> ""
        }

        val remainingCount = if (isCompleted) {
            0
        } else if (ppt > 0 && commYear > 0) {
            val nextCal = parseDate(nextPaymentDate)?.let { Calendar.getInstance().apply { time = it } }
            val nextYear = nextCal?.get(Calendar.YEAR) ?: (commYear + 1)
            val finalPaymentYear = commYear + ppt - 1
            val rem = finalPaymentYear - nextYear + 1
            if (rem < 0) 0 else rem
        } else if (storedLastPremium.isNotBlank()) {
            val endCal = parseDate(storedLastPremium)?.let { Calendar.getInstance().apply { time = it } }
            val nextCal = parseDate(nextPaymentDate)?.let { Calendar.getInstance().apply { time = it } }
            if (endCal != null && nextCal != null) {
                val rem = endCal.get(Calendar.YEAR) - nextCal.get(Calendar.YEAR)
                if (rem < 0) 0 else rem
            } else 0
        } else {
            0
        }

        return PolicyScheduleInfo(
            termYears = term,
            pptYears = ppt,
            commencementDate = commDate,
            endOfPremiumTermDate = endOfPpt,
            maturityDate = matDate,
            remainingPaymentsCount = remainingCount,
            isFullyPaid = isCompleted || remainingCount <= 0
        )
    }

    fun calculateNextPaymentAdvance(
        currentNextDate: String,
        totalYears: String = "",
        forceMatured: Boolean = false,
        lastPaymentDate: String = "",
        storedLastPremiumDate: String = "",
        storedCommencementDate: String = ""
    ): NextPaymentAdvanceResult {
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        if (forceMatured) {
            return NextPaymentAdvanceResult(
                newNextPaymentDate = "Completed",
                newLastPaymentDate = todayStr,
                isMatured = true
            )
        }

        val parsed = parseDate(currentNextDate)
        if (parsed == null) {
            return NextPaymentAdvanceResult(
                newNextPaymentDate = currentNextDate,
                newLastPaymentDate = todayStr,
                isMatured = false
            )
        }

        val cal = Calendar.getInstance().apply {
            time = parsed
        }
        val currentDueYear = cal.get(Calendar.YEAR)
        cal.add(Calendar.YEAR, 1)
        val nextYear = cal.get(Calendar.YEAR)
        val advancedDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)

        var isMatured = false

        val (term, ppt) = parseTermAndPpt(totalYears)
        if (ppt > 0) {
            val commYear = when {
                storedCommencementDate.isNotBlank() -> {
                    parseDate(storedCommencementDate)?.let { Calendar.getInstance().apply { time = it }.get(Calendar.YEAR) } ?: (currentDueYear - 1)
                }
                lastPaymentDate.isNotBlank() -> {
                    parseDate(lastPaymentDate)?.let { Calendar.getInstance().apply { time = it }.get(Calendar.YEAR) } ?: (currentDueYear - 1)
                }
                else -> currentDueYear - 1
            }
            val finalPaymentYear = commYear + ppt - 1
            if (currentDueYear >= finalPaymentYear || nextYear > finalPaymentYear) {
                isMatured = true
            }
        } else if (storedLastPremiumDate.isNotBlank()) {
            val expCal = parseDate(storedLastPremiumDate)?.let { Calendar.getInstance().apply { time = it } }
            if (expCal != null && (cal.after(expCal) || nextYear >= expCal.get(Calendar.YEAR))) {
                isMatured = true
            }
        } else {
            val cleanTotal = totalYears.trim()
            val expiryDate = parseDate(cleanTotal)
            if (expiryDate != null) {
                val expCal = Calendar.getInstance().apply { time = expiryDate }
                if (cal.after(expCal) || cal.get(Calendar.YEAR) >= expCal.get(Calendar.YEAR)) {
                    isMatured = true
                }
            } else {
                val yearRegex = Regex("""\b(20[2-9][0-9])\b""")
                val match = yearRegex.find(cleanTotal)
                if (match != null) {
                    val expYear = match.value.toIntOrNull()
                    if (expYear != null && nextYear >= expYear) {
                        isMatured = true
                    }
                }
            }
        }

        return NextPaymentAdvanceResult(
            newNextPaymentDate = if (isMatured) "Completed" else advancedDateStr,
            newLastPaymentDate = todayStr,
            isMatured = isMatured
        )
    }
}
