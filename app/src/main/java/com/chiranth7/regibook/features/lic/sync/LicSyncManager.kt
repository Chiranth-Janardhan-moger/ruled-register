package com.chiranth7.regibook.features.lic.sync

import android.content.Context
import com.chiranth7.regibook.data.AppDatabase
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.worker.DailyLicReminderWorker
import com.chiranth7.regibook.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LicSyncManager {

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }

    suspend fun syncPolicies(
        context: Context,
        settingsManager: SettingsManager,
        customUrl: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        val targetUrl = (customUrl ?: settingsManager.policySyncUrl.value).trim()
        if (targetUrl.isBlank()) {
            return@withContext SyncResult.Error("No sync URL configured")
        }

        try {
            val jsonText = fetchUrl(targetUrl)
            val accounts = parsePoliciesJson(jsonText)
            if (accounts.isEmpty()) {
                return@withContext SyncResult.Error("No valid policies found in response")
            }

            val database = AppDatabase.getDatabase(context)
            val licDao = database.licDao()

            var updatedCount = 0
            for (incoming in accounts) {
                if (incoming.policyNumber.isBlank()) continue

                val existing = licDao.getAccountByPolicyNumber(incoming.policyNumber)
                if (existing != null) {
                    val updated = existing.copy(
                        name = incoming.name.ifBlank { existing.name },
                        policyName = incoming.policyName.ifBlank { existing.policyName },
                        totalYears = incoming.totalYears.ifBlank { existing.totalYears },
                        phoneNumber = incoming.phoneNumber.ifBlank { existing.phoneNumber },
                        lastPaymentDate = incoming.lastPaymentDate.ifBlank { existing.lastPaymentDate },
                        nextPaymentDate = incoming.nextPaymentDate.ifBlank { existing.nextPaymentDate },
                        premiumAmount = incoming.premiumAmount.ifBlank { existing.premiumAmount },
                        address = incoming.address.ifBlank { existing.address }
                    )
                    licDao.updateAccount(updated)
                } else {
                    licDao.insertAccount(incoming)
                }
                updatedCount++
            }

            settingsManager.setLastSyncTimestamp(System.currentTimeMillis())

            // Immediately check and post any due reminders for newly updated policies
            try {
                DailyLicReminderWorker.checkAndPostReminders(context)
            } catch (_: Exception) {}

            com.chiranth7.regibook.util.log.AppLogManager.log(context, "LicSync", "Synced $updatedCount policies successfully")
            SyncResult.Success(updatedCount)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to sync policies"
            com.chiranth7.regibook.util.log.AppLogManager.log(
                context,
                "LicSync",
                "Sync failed: $errorMsg",
                isError = true,
                throwable = e
            )
            com.chiranth7.regibook.util.log.AppLogManager.scheduleRetryOnConnectivity(context)
            SyncResult.Error(errorMsg)
        }
    }

    private fun fetchUrl(urlStr: String): String {
        var currentUrl = urlStr
        var redirects = 0
        val maxRedirects = 5

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "RegiBook-Android-App")
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw java.io.IOException("Redirect with no Location header")
                currentUrl = location
                redirects++
                connection.disconnect()
                continue
            }

            if (responseCode !in 200..299) {
                connection.disconnect()
                throw java.io.IOException("Server returned HTTP $responseCode")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val sb = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append('\n')
            }
            reader.close()
            connection.disconnect()
            return sb.toString()
        }
        throw java.io.IOException("Too many redirects")
    }

    private fun parsePoliciesJson(jsonString: String): List<LicAccount> {
        val trimmed = jsonString.trim()
        val jsonArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("policies") -> obj.getJSONArray("policies")
                    obj.has("accounts") -> obj.getJSONArray("accounts")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> JSONArray()
                }
            }
            else -> throw IllegalArgumentException("Response is not valid JSON")
        }

        val list = mutableListOf<LicAccount>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue

            val policyNumber = item.optString("policyNumber", item.optString("policy_number", "")).trim()
            val name = item.optString("name", item.optString("customerName", item.optString("customer_name", ""))).trim()
            if (policyNumber.isBlank() && name.isBlank()) continue

            val policyName = item.optString("policyName", item.optString("policy_name", item.optString("plan", ""))).trim()
            val totalYears = item.optString("totalYears", item.optString("total_years", item.optString("termPpt", item.optString("term_ppt", "")))).trim()
            val phoneNumber = item.optString("phoneNumber", item.optString("phone_number", item.optString("phone", ""))).trim()
            val lastPaymentDate = item.optString("lastPaymentDate", item.optString("last_payment_date", item.optString("last_paid", ""))).trim()
            val nextPaymentDate = item.optString("nextPaymentDate", item.optString("next_payment_date", item.optString("next_due_date", item.optString("fup", "")))).trim()
            val premiumAmount = item.optString("premiumAmount", item.optString("premium_amount", item.optString("premium", ""))).trim()
            val sumAssured = item.optString("sumAssured", item.optString("sum_assured", item.optString("address", ""))).trim()

            list.add(
                LicAccount(
                    id = 0L,
                    name = name,
                    policyNumber = policyNumber,
                    policyName = policyName,
                    totalYears = totalYears,
                    phoneNumber = phoneNumber,
                    lastPaymentDate = lastPaymentDate,
                    nextPaymentDate = nextPaymentDate,
                    premiumAmount = premiumAmount,
                    address = sumAssured
                )
            )
        }
        return list
    }
}
