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

    fun isHalfYearly(
        premiumAmount: String = "",
        lastPaymentDate: String = "",
        nextPaymentDate: String = ""
    ): Boolean {
        val cleanAmount = premiumAmount.lowercase()
        if (cleanAmount.contains("half") ||
            cleanAmount.contains("6 month") ||
            cleanAmount.contains("6month") ||
            cleanAmount.contains("6-month") ||
            cleanAmount.contains("semi")
        ) {
            return true
        }
        if (lastPaymentDate.isNotBlank() && nextPaymentDate.isNotBlank()) {
            val lastD = parseDate(lastPaymentDate)
            val nextD = parseDate(nextPaymentDate)
            if (lastD != null && nextD != null) {
                val calLast = Calendar.getInstance().apply { time = lastD }
                val calNext = Calendar.getInstance().apply { time = nextD }
                val monthsDiff = Math.abs(
                    (calNext.get(Calendar.YEAR) - calLast.get(Calendar.YEAR)) * 12 +
                            (calNext.get(Calendar.MONTH) - calLast.get(Calendar.MONTH))
                )
                if (monthsDiff in 5..7) {
                    return true
                }
            }
        }
        return false
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
        val remainingYears: Int = 0,
        val remainingMonths: Int = 0,
        val isHalfYearly: Boolean = false,
        val isFullyPaid: Boolean
    )

    fun calculatePolicySchedule(
        totalYears: String,
        nextPaymentDate: String,
        lastPaymentDate: String = "",
        storedCommencement: String = "",
        storedLastPremium: String = "",
        storedMaturity: String = "",
        premiumAmount: String = ""
    ): PolicyScheduleInfo? {
        val (term, ppt) = parseTermAndPpt(totalYears)
        if (term <= 0 && ppt <= 0 && storedLastPremium.isBlank() && storedMaturity.isBlank()) {
            return null
        }

        val isCompleted = nextPaymentDate.trim().equals("Completed", ignoreCase = true) ||
                nextPaymentDate.trim().contains("Matured", ignoreCase = true)

        val isHalf = isHalfYearly(premiumAmount, lastPaymentDate, nextPaymentDate)

        val commDate = when {
            storedCommencement.isNotBlank() -> storedCommencement.trim()
            lastPaymentDate.isNotBlank() && parseDate(lastPaymentDate) != null -> {
                val p = parseDate(lastPaymentDate)!!
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(p)
            }
            nextPaymentDate.isNotBlank() && !isCompleted -> {
                val p = parseDate(nextPaymentDate)
                if (p != null) {
                    val cal = Calendar.getInstance().apply {
                        time = p
                        if (isHalf) add(Calendar.MONTH, -6) else add(Calendar.YEAR, -1)
                    }
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
        } else if (ppt > 0) {
            val totalPayments = if (isHalf) ppt * 2 else ppt
            val nextCal = parseDate(nextPaymentDate)?.let { Calendar.getInstance().apply { time = it } }
            if (nextCal != null && commCal != null) {
                val elapsedMonths = (nextCal.get(Calendar.YEAR) - commCal.get(Calendar.YEAR)) * 12 +
                        (nextCal.get(Calendar.MONTH) - commCal.get(Calendar.MONTH))
                val intervalMonths = if (isHalf) 6 else 12
                val elapsedPayments = (elapsedMonths / intervalMonths).coerceAtLeast(0)
                val rem = totalPayments - elapsedPayments
                if (rem < 0) 0 else rem
            } else if (nextCal != null && commYear > 0) {
                val nextYear = nextCal.get(Calendar.YEAR)
                val finalPaymentYear = commYear + ppt - 1
                val remYears = finalPaymentYear - nextYear + 1
                val rem = if (isHalf) remYears * 2 else remYears
                if (rem < 0) 0 else rem
            } else {
                0
            }
        } else if (storedLastPremium.isNotBlank()) {
            val endCal = parseDate(storedLastPremium)?.let { Calendar.getInstance().apply { time = it } }
            val nextCal = parseDate(nextPaymentDate)?.let { Calendar.getInstance().apply { time = it } }
            if (endCal != null && nextCal != null) {
                val monthsDiff = (endCal.get(Calendar.YEAR) - nextCal.get(Calendar.YEAR)) * 12 +
                        (endCal.get(Calendar.MONTH) - nextCal.get(Calendar.MONTH))
                val intervalMonths = if (isHalf) 6 else 12
                val rem = (monthsDiff / intervalMonths)
                if (rem < 0) 0 else rem
            } else 0
        } else {
            0
        }

        val remYears: Int
        val remMonths: Int
        if (isHalf) {
            remYears = remainingCount / 2
            remMonths = (remainingCount % 2) * 6
        } else {
            remYears = remainingCount
            remMonths = 0
        }

        return PolicyScheduleInfo(
            termYears = term,
            pptYears = ppt,
            commencementDate = commDate,
            endOfPremiumTermDate = endOfPpt,
            maturityDate = matDate,
            remainingPaymentsCount = remainingCount,
            remainingYears = remYears,
            remainingMonths = remMonths,
            isHalfYearly = isHalf,
            isFullyPaid = isCompleted || remainingCount <= 0
        )
    }

    fun calculateNextPaymentAdvance(
        currentNextDate: String,
        totalYears: String = "",
        forceMatured: Boolean = false,
        lastPaymentDate: String = "",
        storedLastPremiumDate: String = "",
        storedCommencementDate: String = "",
        premiumAmount: String = ""
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

        val isHalf = isHalfYearly(premiumAmount, lastPaymentDate, currentNextDate)
        val cal = Calendar.getInstance().apply {
            time = parsed
        }
        val currentDueYear = cal.get(Calendar.YEAR)
        if (isHalf) {
            cal.add(Calendar.MONTH, 6)
        } else {
            cal.add(Calendar.YEAR, 1)
        }
        val nextYear = cal.get(Calendar.YEAR)
        val advancedDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)

        var isMatured = false

        val (term, ppt) = parseTermAndPpt(totalYears)
        if (ppt > 0) {
            val commDateParsed = when {
                storedCommencementDate.isNotBlank() -> parseDate(storedCommencementDate)
                lastPaymentDate.isNotBlank() -> parseDate(lastPaymentDate)
                else -> null
            }
            val commCal = commDateParsed?.let { Calendar.getInstance().apply { time = it } }

            if (isHalf) {
                val totalPayments = ppt * 2
                val finalPaymentIndex = totalPayments - 1
                if (commCal != null) {
                    val origCal = Calendar.getInstance().apply { time = parsed }
                    val currentPaymentIndex = ((origCal.get(Calendar.YEAR) - commCal.get(Calendar.YEAR)) * 12 +
                            (origCal.get(Calendar.MONTH) - commCal.get(Calendar.MONTH))) / 6
                    if (currentPaymentIndex >= finalPaymentIndex) {
                        isMatured = true
                    }
                } else {
                    val commYear = currentDueYear - 1
                    val finalPaymentYear = commYear + ppt - 1
                    if (currentDueYear >= finalPaymentYear) {
                        isMatured = true
                    }
                }
            } else {
                val commYear = commCal?.get(Calendar.YEAR) ?: (currentDueYear - 1)
                val finalPaymentYear = commYear + ppt - 1
                if (currentDueYear >= finalPaymentYear || nextYear > finalPaymentYear) {
                    isMatured = true
                }
            }
        } else if (storedLastPremiumDate.isNotBlank()) {
            val expCal = parseDate(storedLastPremiumDate)?.let { Calendar.getInstance().apply { time = it } }
            if (expCal != null) {
                val origCal = Calendar.getInstance().apply { time = parsed }
                if (!origCal.before(expCal) || cal.after(expCal)) {
                    isMatured = true
                }
            }
        } else {
            val cleanTotal = totalYears.trim()
            val expiryDate = parseDate(cleanTotal)
            if (expiryDate != null) {
                val expCal = Calendar.getInstance().apply { time = expiryDate }
                if (cal.after(expCal) || !cal.before(expCal)) {
                    isMatured = true
                }
            } else {
                val yearRegex = Regex("""\b(20[2-9][0-9])\b""")
                val match = yearRegex.find(cleanTotal)
                if (match != null) {
                    val expYear = match.value.toIntOrNull()
                    if (expYear != null && cal.get(Calendar.YEAR) >= expYear) {
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
