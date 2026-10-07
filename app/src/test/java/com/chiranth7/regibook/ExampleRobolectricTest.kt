package com.chiranth7.regibook

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.features.lic.util.PaymentReminderStatus
import com.chiranth7.regibook.util.RegisterType
import com.chiranth7.regibook.util.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
  fun `launch MainActivity test`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
  }
}
