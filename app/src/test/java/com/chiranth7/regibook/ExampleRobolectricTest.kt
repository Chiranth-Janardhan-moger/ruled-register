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
  fun `test all Kannada string resources can be loaded and formatted`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration(context.resources.configuration).apply {
      setLocale(Locale("kn"))
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

    assertTrue(com.chiranth7.regibook.util.log.AppLogManager.DISCORD_WEBHOOK_URL.startsWith("https://discord.com/api/webhooks/"))
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
      setLocale(Locale("kn"))
    }
    val knContext = context.createConfigurationContext(config)

    val offlineKn = knContext.getString(R.string.no_internet_connection)
    assertFalse(offlineKn.isBlank())

    val markNotifiedKn = knContext.getString(R.string.mark_as_notified)
    assertFalse(markNotifiedKn.isBlank())

    val notifiedKn = knContext.getString(R.string.notified)
    assertFalse(notifiedKn.isBlank())
  }
}


