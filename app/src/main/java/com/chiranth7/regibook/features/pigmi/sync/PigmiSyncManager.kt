package com.chiranth7.regibook.features.pigmi.sync

import android.content.Context
import com.chiranth7.regibook.data.AppDatabase
import com.chiranth7.regibook.features.pigmi.data.PigmiAccount
import com.chiranth7.regibook.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object PigmiSyncManager {

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }

    private fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun syncPigmi(
        context: Context,
        settingsManager: SettingsManager,
        customUrl: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!isOnline(context)) {
            return@withContext SyncResult.Error(context.getString(com.chiranth7.regibook.R.string.no_internet_connection))
        }

        val targetUrl = (customUrl ?: settingsManager.pigmiSyncUrl.value).trim()
        if (targetUrl.isBlank()) {
            return@withContext SyncResult.Error("No sync URL configured")
        }

        try {
            val jsonText = fetchUrl(targetUrl)
            val accounts = parsePigmiJson(jsonText)
            if (accounts.isEmpty()) {
                return@withContext SyncResult.Error("No valid pigmi records found in response")
            }

            val database = AppDatabase.getDatabase(context)
            val pigmiDao = database.pigmiDao()

            var updatedCount = 0
            for (incoming in accounts) {
                if (incoming.srNo <= 0) continue

                val existing = pigmiDao.getAccountBySrNo(incoming.srNo)
                if (existing != null) {
                    val updated = existing.copy(
                        name = incoming.name.ifBlank { existing.name },
                        kannadaName = incoming.kannadaName.ifBlank { existing.kannadaName }
                    )
                    pigmiDao.updateAccount(updated)
                } else {
                    pigmiDao.insertAccount(incoming)
                }
                updatedCount++
            }

            com.chiranth7.regibook.util.log.AppLogManager.log(context, "PigmiSync", "Synced $updatedCount pigmi records successfully")
            SyncResult.Success(updatedCount)
        } catch (e: Exception) {
            val isNetworkIssue = !isOnline(context) || e is java.io.IOException || e is java.net.UnknownHostException
            val errorMsg = if (isNetworkIssue) {
                context.getString(com.chiranth7.regibook.R.string.no_internet_connection)
            } else {
                e.message ?: "Failed to sync pigmi records"
            }
            com.chiranth7.regibook.util.log.AppLogManager.log(
                context,
                "PigmiSync",
                "Sync failed: $errorMsg",
                isError = true,
                throwable = e
            )
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
            val sb = StringBuilder()
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

    private fun parsePigmiJson(jsonString: String): List<PigmiAccount> {
        val trimmed = jsonString.trim()
        val jsonArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("pigmi") -> obj.getJSONArray("pigmi")
                    obj.has("accounts") -> obj.getJSONArray("accounts")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> JSONArray()
                }
            }
            else -> throw IllegalArgumentException("Response is not valid JSON")
        }

        val list = mutableListOf<PigmiAccount>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue

            val srNo = item.optInt("srNo", item.optInt("sr_no", item.optInt("id", 0)))
            val name = item.optString("name", item.optString("customerName", item.optString("customer_name", ""))).trim()
            val kannadaName = item.optString("kannadaName", item.optString("kannada_name", item.optString("nameKn", ""))).trim()

            if (srNo <= 0 && name.isBlank()) continue

            list.add(
                PigmiAccount(
                    id = 0L,
                    srNo = srNo,
                    name = name,
                    phoneNumber = "",
                    address = "",
                    accountNumber = "",
                    dailyAmount = "",
                    kannadaName = kannadaName
                )
            )
        }
        return list
    }
}
