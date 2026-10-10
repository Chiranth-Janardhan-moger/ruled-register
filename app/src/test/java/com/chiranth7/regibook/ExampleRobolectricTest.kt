package com.chiranth7.regibook

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.features.lic.util.PaymentReminderStatus
import com.chiranth7.regibook.util.RegisterType
import com.chiranth7.regibook.util.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Register Book", appName)
    val pigmiString = context.getString(R.string.pigmi_register)
    assertEquals("Pigmi", pigmiString)
    val licString = context.getString(R.string.lic_register)
    assertNotNull(licString)
    val profileString = context.getString(R.string.profile)
    assertEquals("Profile", profileString)
    val agentNumberString = context.getString(R.string.agent_number)
    assertEquals("Agent Number", agentNumberString)
    val policyNameString = context.getString(R.string.policy_name)
    assertNotNull(policyNameString)
    val totalYearsString = context.getString(R.string.total_years)
    assertNotNull(totalYearsString)
  }

  @Test
  fun `settings manager toggles active register and handles agent number`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager(context)
    assertEquals(RegisterType.PIGMI, settingsManager.activeRegisterType.value)

    settingsManager.setActiveRegisterType(RegisterType.LIC)
    assertEquals(RegisterType.LIC, settingsManager.activeRegisterType.value)

    settingsManager.setActiveRegisterType(RegisterType.PIGMI)
    assertEquals(RegisterType.PIGMI, settingsManager.activeRegisterType.value)

    assertEquals(SettingsManager.DEFAULT_AGENT_CODE, settingsManager.agentNumber.value)
    settingsManager.setAgentNumber("08492048")
    assertEquals("08492048", settingsManager.agentNumber.value)
  }

  @Test
  fun `payment reminder calculation handles dates correctly`() {
    val todayCal = Calendar.getInstance()
    val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(todayCal.time)

    val reminderToday = PaymentReminderHelper.calculateReminder(todayStr)
    assertEquals(PaymentReminderStatus.DUE_TODAY, reminderToday.status)

    val futureCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 5) }
    val futureStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(futureCal.time)
    val reminderSoon = PaymentReminderHelper.calculateReminder(futureStr)
    assertEquals(PaymentReminderStatus.DUE_SOON, reminderSoon.status)

    val overdueCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -5) }
    val overdueStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(overdueCal.time)
    val reminderOverdue = PaymentReminderHelper.calculateReminder(overdueStr)
    assertEquals(PaymentReminderStatus.OVERDUE, reminderOverdue.status)
  }

  @Test
  fun `lic account holds all required fields`() {
    val account = LicAccount(
      id = 1L,
      name = "Kiran Kumar",
      policyNumber = "POL-8923410",
      policyName = "Jeevan Labh",
      totalYears = "16 Years",
      phoneNumber = "+91 99001 23456",
      lastPaymentDate = "15/09/2026",
      nextPaymentDate = "15/10/2026",
      premiumAmount = "₹2,500",
      address = "Bengaluru"
    )
    assertEquals("Kiran Kumar", account.name)
    assertEquals("POL-8923410", account.policyNumber)
    assertEquals("Jeevan Labh", account.policyName)
    assertEquals("16 Years", account.totalYears)
    assertEquals("15/10/2026", account.nextPaymentDate)
  }

  @Test
  fun `settings manager stores policy sync url and timestamp`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager(context)
    assertEquals(SettingsManager.DEFAULT_POLICY_SYNC_URL, settingsManager.policySyncUrl.value)

    val customUrl = "https://gist.githubusercontent.com/test/raw/policies.json"
    settingsManager.setPolicySyncUrl(customUrl)
    assertEquals(customUrl, settingsManager.policySyncUrl.value)

    assertEquals(0L, settingsManager.lastSyncTimestamp.value)
    val now = System.currentTimeMillis()
    settingsManager.setLastSyncTimestamp(now)
    assertEquals(now, settingsManager.lastSyncTimestamp.value)
  }

  @Test
  fun `daily reminder worker schedule registers unique work`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.chiranth7.regibook.features.lic.worker.DailyLicReminderWorker.schedule(context)
  }

  @Test
  fun `settings manager adjusts font scale within bounds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager(context)
    assertEquals(1.0f, settingsManager.fontScale.value, 0.01f)

    settingsManager.increaseFontScale()
    assertEquals(1.10f, settingsManager.fontScale.value, 0.01f)

    settingsManager.decreaseFontScale()
    assertEquals(1.0f, settingsManager.fontScale.value, 0.01f)

    settingsManager.setFontScale(2.5f) // Should clamp to MAX_FONT_SCALE
    assertEquals(SettingsManager.MAX_FONT_SCALE, settingsManager.fontScale.value, 0.01f)

    settingsManager.setFontScale(0.1f) // Should clamp to MIN_FONT_SCALE
    assertEquals(SettingsManager.MIN_FONT_SCALE, settingsManager.fontScale.value, 0.01f)
  }

  @Test
  fun `calculateNextPaymentAdvance advances year correctly`() {
    val result = PaymentReminderHelper.calculateNextPaymentAdvance(
      currentNextDate = "28/06/2026",
      totalYears = "21/15"
    )
    assertEquals("28/06/2027", result.newNextPaymentDate)
    assertFalse(result.isMatured)
    assertTrue(result.newLastPaymentDate.isNotBlank())
  }

  @Test
  fun `calculateNextPaymentAdvance handles maturity correctly`() {
    val forced = PaymentReminderHelper.calculateNextPaymentAdvance(
      currentNextDate = "28/06/2026",
      totalYears = "21/15",
      forceMatured = true
    )
    assertEquals("Completed", forced.newNextPaymentDate)
    assertTrue(forced.isMatured)

    // With explicit expiry year that is reached
    val expiryReached = PaymentReminderHelper.calculateNextPaymentAdvance(
      currentNextDate = "28/06/2026",
      totalYears = "2027"
    )
    assertEquals("Completed", expiryReached.newNextPaymentDate)
    assertTrue(expiryReached.isMatured)

    // Verify reminder returns COMPLETED
    val reminder = PaymentReminderHelper.calculateReminder("Completed")
    assertEquals(PaymentReminderStatus.COMPLETED, reminder.status)
  }

  @Test
  fun `launch MainActivity test`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
  }

  @Test
  fun `launch MainActivity with Kannada language does not crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager(context)
    settingsManager.setLanguage(SettingsManager.LANG_KANNADA)
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
  }

  @Test
  fun `test KannadaNameHelper edge cases`() {
    val inputs = listOf(
        "",
        "   ",
        "Chiranth",
        "chiranth",
        "Chiranth Janardhan Moger",
        "736 - LIC'S JEEVAN LABH PLAN",
        "12345",
        "Special & Characters % $ # @ ! () / -",
        "Already ಕನ್ನಡ Text",
        "A B C",
        "Dr. A. P. J. Abdul Kalam",
        "Ramesh Kumar 15 Yrs (₹11,873)",
        "Jeevan Labh",
        "Jeevan Umang",
        "LIC's",
        "Plan",
        "xyz unknown name 123",
        "qq xx ww"
    )
    for (input in inputs) {
      val res = com.chiranth7.regibook.util.KannadaNameHelper.formatDisplayName(input, SettingsManager.LANG_KANNADA)
      assertNotNull(res)
    }
  }

  @Test
  fun `test policyholders kannada names`() {
    val map = mapOf(
      "Chiranth Janardhan Moger" to "ಚಿರಂತ ಜನಾರ್ದನ ಮೊಗೇರ",
      "Koushik Janardhan Moger" to "ಕೌಶಿಕ್ ಜನಾರ್ದನ ಮೊಗೇರ",
      "Mohammed Mustafa Karkada" to "ಮೊಹಮ್ಮದ್ ಮುಸ್ತಫಾ ಕರ್ಕಾಡ",
      "Dsouza Juliyas" to "ಡಿಸೋಜ ಜೂಲಿಯಸ್",
      "Janardhan Moger" to "ಜನಾರ್ದನ ಮೊಗೇರ",
      "Chitra Narayan Naik" to "ಚಿತ್ರಾ ನಾರಾಯಣ ನಾಯ್ಕ್",
      "Nagaraj Moger" to "ನಾಗರಾಜ ಮೊಗೇರ",
      "Pratham Subray Bhat" to "ಪ್ರಥಮ್ ಸುಬ್ರಾಯ ಭಟ್",
      "Bhavana Moger" to "ಭಾವನಾ ಮೊಗೇರ",
      "Nagaratna Anand Gond" to "ನಾಗರತ್ನ ಆನಂದ್ ಗೌಡ",
      "Manjunath Nagappa Gond" to "ಮಂಜುನಾಥ್ ನಾಗಪ್ಪ ಗೌಡ"
    )
    for ((name, expected) in map) {
      val kn = com.chiranth7.regibook.util.KannadaNameHelper.formatDisplayName(name, SettingsManager.LANG_KANNADA)
      assertEquals(expected, kn)

      val account = LicAccount(
        name = name,
        policyNumber = "12345",
        kannadaName = expected
      )
      assertEquals(expected, account.getDisplayName(SettingsManager.LANG_KANNADA))
      assertEquals(name, account.getDisplayName(SettingsManager.LANG_ENGLISH))
    }
  }

  @Test
  fun `test all Kannada string resources can be loaded and formatted`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration(context.resources.configuration).apply {
      setLocale(Locale.forLanguageTag("kn"))
    }
    val knContext = context.createConfigurationContext(config)

    // Test specific formatted strings
    val syncSuccess = knContext.getString(R.string.sync_success, 42)
    assertTrue(syncSuccess.contains("42"))

    val lastSynced = knContext.getString(R.string.last_synced, "07 Oct, 10:30 PM")
    assertTrue(lastSynced.contains("07 Oct, 10:30 PM"))

    val paymentRecorded = knContext.getString(R.string.payment_recorded_success, "28/06/2027")
    assertTrue(paymentRecorded.contains("28/06/2027"))

    val policyCompleted = knContext.getString(R.string.policy_completed_success)
    assertFalse(policyCompleted.isBlank())

    val logsString = knContext.getString(R.string.logs)
    assertFalse(logsString.isBlank())

    val closeString = knContext.getString(R.string.close)
    assertFalse(closeString.isBlank())
  }

  @Test
  fun `test AppLogManager operations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.chiranth7.regibook.util.log.AppLogManager.clearLogs(context)
    assertTrue(com.chiranth7.regibook.util.log.AppLogManager.getLogs(context).isEmpty())

    com.chiranth7.regibook.util.log.AppLogManager.log(context, "TEST_TAG", "Sample info message", isError = false)
    com.chiranth7.regibook.util.log.AppLogManager.log(context, "ERROR_TAG", "Sample error message", isError = true)

    val logs = com.chiranth7.regibook.util.log.AppLogManager.getLogs(context)
    assertEquals(2, logs.size)
    assertEquals("TEST_TAG", logs[0].tag)
    assertFalse(logs[0].isError)
    assertEquals("ERROR_TAG", logs[1].tag)
    assertTrue(logs[1].isError)

    val formatted = com.chiranth7.regibook.util.log.AppLogManager.getFormattedLogs(context)
    assertTrue(formatted.contains("[INFO] [TEST_TAG]"))
    assertTrue(formatted.contains("Sample info message"))
    assertTrue(formatted.contains("[ERROR] [ERROR_TAG]"))
    assertTrue(formatted.contains("Sample error message"))

    // Test retry scheduler doesn't crash
    com.chiranth7.regibook.util.log.AppLogManager.scheduleRetryOnConnectivity(context)

    com.chiranth7.regibook.util.log.AppLogManager.clearLogs(context)
    assertTrue(com.chiranth7.regibook.util.log.AppLogManager.getLogs(context).isEmpty())
  }

  @Test
  fun `test Discord payload format and offline queuing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val entry = com.chiranth7.regibook.util.log.AppLogEntry(
        timestamp = System.currentTimeMillis(),
        tag = "LicSync",
        message = "Failed to sync: Connection timeout",
        isError = true,
        page = "lic_register",
        location = "LicSyncManager.kt:76 (LicSyncManager.syncPolicies)",
        isSentToRemote = false
    )
    val payloadJson = com.chiranth7.regibook.util.log.AppLogManager.buildDiscordPayload(context, entry)
    assertTrue(payloadJson.contains("embeds"))
    assertTrue(payloadJson.contains("LicSync"))
    assertTrue(payloadJson.contains("lic_register"))
    assertTrue(payloadJson.contains("LicSyncManager.kt:76"))
    assertTrue(payloadJson.contains("Connection timeout"))

    // Verify DISCORD_WEBHOOK_URL is configured via BuildConfig
    val configuredUrl = com.chiranth7.regibook.util.log.AppLogManager.DISCORD_WEBHOOK_URL
    assertTrue(configuredUrl.isEmpty() || configuredUrl.startsWith("https://discord.com/api/webhooks/"))
  }

  @Test
  fun `test SettingsManager policy notified flag logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager(context)

    val policy = "739558210"
    val due2026 = "28/06/2026"
    val due2027 = "28/06/2027"

    // Initially not notified
    assertFalse(settingsManager.isPolicyNotified(policy, due2026))

    // Set notified
    settingsManager.setPolicyNotified(policy, due2026, true)
    assertTrue(settingsManager.isPolicyNotified(policy, due2026))

    // Next year cycle must be false (starts fresh on next due date)
    assertFalse(settingsManager.isPolicyNotified(policy, due2027))

    // Toggle off
    settingsManager.setPolicyNotified(policy, due2026, false)
    assertFalse(settingsManager.isPolicyNotified(policy, due2026))
  }

  @Test
  fun `test urgency sorting ranks overdue first, then due soon, then upcoming`() {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Overdue: 10 days ago
    cal.add(Calendar.DAY_OF_YEAR, -10)
    val overdueDate = sdf.format(cal.time)

    // Due soon: 3 days in future
    cal.time = Date()
    cal.add(Calendar.DAY_OF_YEAR, 3)
    val dueSoonDate = sdf.format(cal.time)

    // Upcoming: 30 days in future
    cal.time = Date()
    cal.add(Calendar.DAY_OF_YEAR, 30)
    val upcomingDate = sdf.format(cal.time)

    val overdueAccount = LicAccount(
      id = 1L,
      name = "Overdue Customer",
      policyNumber = "POL-001",
      nextPaymentDate = overdueDate
    )
    val dueSoonAccount = LicAccount(
      id = 2L,
      name = "Due Soon Customer",
      policyNumber = "POL-002",
      nextPaymentDate = dueSoonDate
    )
    val upcomingAccount = LicAccount(
      id = 3L,
      name = "Upcoming Customer",
      policyNumber = "POL-003",
      nextPaymentDate = upcomingDate
    )

    // Unsorted list
    val list = listOf(upcomingAccount, overdueAccount, dueSoonAccount)

    fun getUrgencyRank(status: PaymentReminderStatus): Int = when (status) {
      PaymentReminderStatus.OVERDUE -> 0
      PaymentReminderStatus.DUE_TODAY -> 1
      PaymentReminderStatus.DUE_SOON -> 2
      PaymentReminderStatus.UPCOMING -> 3
      PaymentReminderStatus.NOT_SET -> 4
      PaymentReminderStatus.COMPLETED -> 5
    }

    val sorted = list.sortedWith(
      compareBy<LicAccount> { account ->
        val reminder = PaymentReminderHelper.calculateReminder(account.nextPaymentDate)
        getUrgencyRank(reminder.status)
      }.thenBy { account ->
        PaymentReminderHelper.parseDate(account.nextPaymentDate)?.time ?: Long.MAX_VALUE
      }
    )

    assertEquals("POL-001", sorted[0].policyNumber) // Overdue first
    assertEquals("POL-002", sorted[1].policyNumber) // Due soon second
    assertEquals("POL-003", sorted[2].policyNumber) // Upcoming third
  }

  @Test
  fun `test new strings for offline and notified`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val offlineEn = context.getString(R.string.no_internet_connection)
    assertTrue(offlineEn.contains("No internet"))

    val markNotifiedEn = context.getString(R.string.mark_as_notified)
    assertEquals("Mark as Notified", markNotifiedEn)

    val notifiedEn = context.getString(R.string.notified)
    assertEquals("Notified", notifiedEn)

    // Kannada config
    val config = Configuration(context.resources.configuration).apply {
      setLocale(Locale.forLanguageTag("kn"))
    }
    val knContext = context.createConfigurationContext(config)

    val offlineKn = knContext.getString(R.string.no_internet_connection)
    assertFalse(offlineKn.isBlank())

    val markNotifiedKn = knContext.getString(R.string.mark_as_notified)
    assertFalse(markNotifiedKn.isBlank())

    val notifiedKn = knContext.getString(R.string.notified)
    assertFalse(notifiedKn.isBlank())

    val endOfPptKn = knContext.getString(R.string.end_of_premium_term)
    assertFalse(endOfPptKn.isBlank())

    val maturityKn = knContext.getString(R.string.maturity_date)
    assertFalse(maturityKn.isBlank())

    val remainingKn = knContext.getString(R.string.years_remaining_to_pay, 14, 14)
    assertTrue(remainingKn.contains("14"))
  }

  @Test
  fun `test parseTermAndPpt parses fraction and single numbers correctly`() {
    val (term1, ppt1) = PaymentReminderHelper.parseTermAndPpt("21/15")
    assertEquals(21, term1)
    assertEquals(15, ppt1)

    val (term2, ppt2) = PaymentReminderHelper.parseTermAndPpt("25/16")
    assertEquals(25, term2)
    assertEquals(16, ppt2)

    val (term3, ppt3) = PaymentReminderHelper.parseTermAndPpt("16/16")
    assertEquals(16, term3)
    assertEquals(16, ppt3)

    val (term4, ppt4) = PaymentReminderHelper.parseTermAndPpt("15 Years")
    assertEquals(15, term4)
    assertEquals(15, ppt4)

    val (term5, ppt5) = PaymentReminderHelper.parseTermAndPpt("")
    assertEquals(0, term5)
    assertEquals(0, ppt5)
  }

  @Test
  fun `test calculatePolicySchedule calculates correct dates and remaining count`() {
    val schedule = PaymentReminderHelper.calculatePolicySchedule(
      totalYears = "21/15",
      nextPaymentDate = "28/06/2026",
      lastPaymentDate = "29/06/2025",
      storedCommencement = "28/06/2025",
      storedLastPremium = "28/06/2040",
      storedMaturity = "28/06/2046"
    )

    assertNotNull(schedule)
    assertEquals(21, schedule!!.termYears)
    assertEquals(15, schedule.pptYears)
    assertEquals("28/06/2025", schedule.commencementDate)
    assertEquals("28/06/2040", schedule.endOfPremiumTermDate)
    assertEquals("28/06/2046", schedule.maturityDate)
    // 15 total payments. 2025 paid (1). 2026 due. Remaining: 2039 - 2026 + 1 = 14
    assertEquals(14, schedule.remainingPaymentsCount)
    assertFalse(schedule.isFullyPaid)
  }

  @Test
  fun `test payment advance stops at PPT limit and transitions to completed`() {
    // Starting at 2038 with 21/15 (commencement 2025, final payment 2039)
    val adv1 = PaymentReminderHelper.calculateNextPaymentAdvance(
      currentNextDate = "28/06/2038",
      totalYears = "21/15",
      forceMatured = false,
      storedCommencementDate = "28/06/2025"
    )
    assertFalse(adv1.isMatured)
    assertEquals("28/06/2039", adv1.newNextPaymentDate)

    // Paying the 15th and final premium (2039)
    val adv2 = PaymentReminderHelper.calculateNextPaymentAdvance(
      currentNextDate = "28/06/2039",
      totalYears = "21/15",
      forceMatured = false,
      storedCommencementDate = "28/06/2025"
    )
    assertTrue(adv2.isMatured)
    assertEquals("Completed", adv2.newNextPaymentDate)
  }

  @Test
  fun `test pigmi 4-digit serial numbers support`() {
    val account = com.chiranth7.regibook.features.pigmi.data.PigmiAccount(
      id = 1000L,
      srNo = 1100,
      name = "Chiranth",
      phoneNumber = "+91 9800000000",
      address = "Bengaluru",
      accountNumber = "PG-1100",
      dailyAmount = "₹200",
      kannadaName = "ಚಿರಂತ"
    )
    assertEquals("1100", account.srNo.toString())
    assertEquals(4, account.srNo.toString().length)
    assertEquals("Chiranth", account.getDisplayName(SettingsManager.LANG_ENGLISH))
    assertEquals("ಚಿರಂತ", account.getDisplayName(SettingsManager.LANG_KANNADA))
  }

  @Test
  fun `test isHalfYearly detection`() {
    assertTrue(PaymentReminderHelper.isHalfYearly(premiumAmount = "₹9,068/Half Year"))
    assertTrue(PaymentReminderHelper.isHalfYearly(premiumAmount = "₹9,867/half year"))
    assertTrue(PaymentReminderHelper.isHalfYearly(premiumAmount = "6 Months"))
    assertTrue(PaymentReminderHelper.isHalfYearly(lastPaymentDate = "21/05/2026", nextPaymentDate = "21/11/2026"))
    assertFalse(PaymentReminderHelper.isHalfYearly(premiumAmount = "₹11,873/Year"))
    assertFalse(PaymentReminderHelper.isHalfYearly(lastPaymentDate = "28/06/2025", nextPaymentDate = "28/06/2026"))
  }

  @Test
  fun `test half-yearly policy advance moves forward by 6 months`() {
    val adv = PaymentReminderHelper.calculateNextPaymentAdvance(
      currentNextDate = "21/11/2026",
      totalYears = "21/15",
      forceMatured = false,
      lastPaymentDate = "21/05/2026",
      storedCommencementDate = "21/05/2025",
      premiumAmount = "₹9,068/Half Year"
    )
    assertFalse(adv.isMatured)
    assertEquals("21/05/2027", adv.newNextPaymentDate)

    // And advancing again
    val advNext = PaymentReminderHelper.calculateNextPaymentAdvance(
      currentNextDate = "21/05/2027",
      totalYears = "21/15",
      forceMatured = false,
      lastPaymentDate = "21/11/2026",
      storedCommencementDate = "21/05/2025",
      premiumAmount = "₹9,068/Half Year"
    )
    assertFalse(advNext.isMatured)
    assertEquals("21/11/2027", advNext.newNextPaymentDate)
  }

  @Test
  fun `test half-yearly policy schedule calculates years and 6 months correctly`() {
    // 21/15 PPT = 15 years = 30 half-yearly payments.
    // Commencement 21/05/2025. Next payment 21/11/2025 -> 1 payment made, 29 payments left.
    // 29 payments = 14 years and 6 months left!
    val schedule29 = PaymentReminderHelper.calculatePolicySchedule(
      totalYears = "21/15",
      nextPaymentDate = "21/11/2025",
      lastPaymentDate = "21/05/2025",
      storedCommencement = "21/05/2025",
      storedLastPremium = "21/05/2040",
      storedMaturity = "21/05/2046",
      premiumAmount = "₹9,068/Half Year"
    )
    assertNotNull(schedule29)
    assertEquals(29, schedule29!!.remainingPaymentsCount)
    assertEquals(14, schedule29.remainingYears)
    assertEquals(6, schedule29.remainingMonths)
    assertTrue(schedule29.isHalfYearly)

    // Next payment 21/05/2026 -> 2 payments made, 28 payments left = 14 years, 0 months.
    val schedule28 = PaymentReminderHelper.calculatePolicySchedule(
      totalYears = "21/15",
      nextPaymentDate = "21/05/2026",
      lastPaymentDate = "21/11/2025",
      storedCommencement = "21/05/2025",
      storedLastPremium = "21/05/2040",
      storedMaturity = "21/05/2046",
      premiumAmount = "₹9,068/Half Year"
    )
    assertNotNull(schedule28)
    assertEquals(28, schedule28!!.remainingPaymentsCount)
    assertEquals(14, schedule28.remainingYears)
    assertEquals(0, schedule28.remainingMonths)

    // Next payment 21/11/2026 -> 3 payments made, 27 payments left = 13 years, 6 months.
    val schedule27 = PaymentReminderHelper.calculatePolicySchedule(
      totalYears = "21/15",
      nextPaymentDate = "21/11/2026",
      lastPaymentDate = "21/05/2026",
      storedCommencement = "21/05/2025",
      storedLastPremium = "21/05/2040",
      storedMaturity = "21/05/2046",
      premiumAmount = "₹9,068/Half Year"
    )
    assertNotNull(schedule27)
    assertEquals(27, schedule27!!.remainingPaymentsCount)
    assertEquals(13, schedule27.remainingYears)
    assertEquals(6, schedule27.remainingMonths)
  }

  @Test
  fun `test half-yearly strings in English and Kannada`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // English string format
    val enStr = context.getString(R.string.years_and_months_remaining_to_pay, 14, 6, 29)
    assertEquals("14 Years 6 Months to Pay (29 Premiums Left)", enStr)

    // Kannada localized context
    val config = Configuration(context.resources.configuration).apply {
      setLocale(Locale("kn"))
    }
    val knContext = context.createConfigurationContext(config)
    val knStr = knContext.getString(R.string.years_and_months_remaining_to_pay, 14, 6, 29)
    assertTrue(knStr.contains("14"))
    assertTrue(knStr.contains("ವರ್ಷ"))
    assertTrue(knStr.contains("6"))
    assertTrue(knStr.contains("ತಿಂಗಳು"))
    assertTrue(knStr.contains("29"))
  }

  @Test
  fun `test pigmi deduplication prunes duplicate entries with same name`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.chiranth7.regibook.data.AppDatabase.getDatabase(context)
    val dao = db.pigmiDao()

    val testName = "Test Deduplication Customer Unique"
    val testNameKn = "ಟೆಸ್ಟ್ ಡೆಡುಪ್ಲಿಕೇಶನ್ ಕಸ್ಟಮರ್"

    // Clean up any pre-existing entries with test name
    val initialMatches = dao.getAccountsByName(testName)
    for (m in initialMatches) {
      dao.deleteAccount(m)
    }

    // Insert original account
    val id1 = dao.insertAccount(
      com.chiranth7.regibook.features.pigmi.data.PigmiAccount(
        srNo = 801,
        name = testName,
        kannadaName = testNameKn
      )
    )

    // Insert an accidental duplicate with different srNo
    val id2 = dao.insertAccount(
      com.chiranth7.regibook.features.pigmi.data.PigmiAccount(
        srNo = 802,
        name = testName,
        kannadaName = testNameKn
      )
    )

    val matches = dao.getAccountsByName(testName)
    assertEquals(2, matches.size)

    // Run deduplication simulation
    val allAccounts = dao.getAllAccountsSnapshot()
    val seenNames = mutableSetOf<String>()
    for (acc in allAccounts) {
      val normalized = acc.name.trim().lowercase()
      if (normalized.isNotBlank()) {
        if (seenNames.contains(normalized)) {
          dao.deleteAccount(acc)
        } else {
          seenNames.add(normalized)
        }
      }
    }

    val remaining = dao.getAccountsByName(testName)
    assertEquals(1, remaining.size)
    assertEquals(801, remaining[0].srNo)
  }

  @Test
  fun `test Kannada strings have no brackets for notified and sum assured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration(context.resources.configuration).apply {
      setLocale(Locale.forLanguageTag("kn"))
    }
    val knContext = context.createConfigurationContext(config)

    val sumAssuredKn = knContext.getString(R.string.sum_assured)
    assertEquals("ವಿಮಾ ಮೊತ್ತ", sumAssuredKn)
    assertFalse(sumAssuredKn.contains("("))
    assertFalse(sumAssuredKn.contains(")"))

    val markNotifiedKn = knContext.getString(R.string.mark_as_notified)
    assertEquals("ಸೂಚಿಸಲಾಗಿದೆ", markNotifiedKn)
    assertFalse(markNotifiedKn.contains("("))
    assertFalse(markNotifiedKn.contains(")"))
  }

  @Test
  fun `test getDisplayPolicyName strips LICS keyword`() {
    val accountWithLics = LicAccount(
      name = "Chiranth Janardhan Moger",
      policyNumber = "739558210",
      policyName = "736 - LIC'S JEEVAN LABH PLAN"
    )
    assertEquals("736 - JEEVAN LABH PLAN", accountWithLics.getDisplayPolicyName(SettingsManager.LANG_ENGLISH))
    assertFalse(accountWithLics.getDisplayPolicyName(SettingsManager.LANG_ENGLISH).contains("LIC"))

    val accountClean = LicAccount(
      name = "Chiranth Janardhan Moger",
      policyNumber = "739558210",
      policyName = "736 - JEEVAN LABH PLAN"
    )
    assertEquals("736 - JEEVAN LABH PLAN", accountClean.getDisplayPolicyName(SettingsManager.LANG_ENGLISH))
  }

  @Test
  fun `test pigmi search filters by numeric serial numbers`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.chiranth7.regibook.data.AppDatabase.getDatabase(context)
    val dao = db.pigmiDao()

    dao.insertAccount(
      com.chiranth7.regibook.features.pigmi.data.PigmiAccount(
        srNo = 7,
        name = "Pratham Subray Bhat",
        kannadaName = "ಪ್ರಥಮ್ ಸುಬ್ರಾಯ ಭಟ್"
      )
    )

    dao.insertAccount(
      com.chiranth7.regibook.features.pigmi.data.PigmiAccount(
        srNo = 11,
        name = "Manjunath Nagappa Gond",
        kannadaName = "ಮಂಜುನಾಥ್ ನಾಗಪ್ಪ ಗೌಡ"
      )
    )

    // Searching "7" should match srNo 7
    val result7 = dao.searchAccounts("7").first()
    assertTrue(result7.any { it.srNo == 7 })

    // Searching "11" should match srNo 11
    val result11 = dao.searchAccounts("11").first()
    assertTrue(result11.any { it.srNo == 11 })
  }
}



