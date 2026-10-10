package com.chiranth7.regibook.util.log

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppLogEntry(
    val timestamp: Long,
    val tag: String,
    val message: String,
    val isError: Boolean,
    val page: String = "home",
    val location: String = "",
    var isSentToRemote: Boolean = false
) {
    fun toFormattedString(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val level = if (isError) "[ERROR]" else "[INFO]"
        val pageStr = if (page.isNotBlank() && page != "home") " @ $page" else ""
        val locStr = if (location.isNotBlank()) " ($location)" else ""
        val sent = if (isSentToRemote) " [DISCORD:SENT]" else ""
        return "${sdf.format(Date(timestamp))} $level [$tag]$pageStr$locStr$sent $message"
    }
}

object AppLogManager {
    private const val LOG_FILE_NAME = "regibook_app_logs.json"
    private const val MAX_LOGS = 50
    private const val RETRY_WORK_TAG = "sync_retry_on_internet"

    @Volatile
    var currentScreen: String = "home"

    val isTestEnvironment: Boolean by lazy {
        try {
            Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
                Class.forName("org.robolectric.Robolectric") != null
        } catch (_: Throwable) {
            false
        }
    }

    const val DISCORD_WEBHOOK_URL =
        "https://discord.com/api/webhooks/1557450236496969849/Db61JCkCLRnGjNM0iLFvfinnxlH4sV05gxX8D1MOGpclOTU-Q-Wj2PHrVyFoqDuc4Eys"

    fun log(
        context: Context,
        tag: String,
        message: String,
        isError: Boolean = false,
        page: String? = null,
        location: String? = null,
        throwable: Throwable? = null
    ) {
        try {
            val resolvedPage = page ?: currentScreen

            val resolvedLocation = when {
                !location.isNullOrBlank() -> location
                throwable != null -> extractLocationFromThrowable(throwable)
                isError -> extractCallerLocation()
                else -> ""
            }

            val file = File(context.filesDir, LOG_FILE_NAME)
            val logs = readLogsInternal(file).toMutableList()
            val entry = AppLogEntry(
                timestamp = System.currentTimeMillis(),
                tag = tag,
                message = message,
                isError = isError,
                page = resolvedPage,
                location = resolvedLocation,
                isSentToRemote = false
            )
            logs.add(entry)
            while (logs.size > MAX_LOGS) {
                logs.removeAt(0)
            }
            writeLogsInternal(file, logs)

            if (isError && !isTestEnvironment) {
                CoroutineScope(Dispatchers.IO).launch {
                    val payload = buildDiscordPayload(context, entry)
                    val sent = sendToDiscord(payload)
                    if (sent) {
                        markEntrySent(context, entry.timestamp)
                    } else {
                        scheduleRetryOnConnectivity(context)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun logCrashSync(context: Context, thread: Thread, throwable: Throwable) {
        if (isTestEnvironment) return
        try {
            val crashDetails = "CRASH on thread [${thread.name}]: ${throwable.javaClass.simpleName}: ${throwable.message ?: "No message"}\n" +
                throwable.stackTraceToString().take(1200)

            val entry = AppLogEntry(
                timestamp = System.currentTimeMillis(),
                tag = "FatalCrash",
                message = crashDetails,
                isError = true,
                page = currentScreen,
                location = extractLocationFromThrowable(throwable),
                isSentToRemote = false
            )

            val file = File(context.filesDir, LOG_FILE_NAME)
            val logs = readLogsInternal(file).toMutableList()
            logs.add(entry)
            while (logs.size > MAX_LOGS) {
                logs.removeAt(0)
            }
            writeLogsInternal(file, logs)

            val payload = buildDiscordPayload(context, entry)
            val netThread = Thread {
                val sent = sendToDiscordSync(payload)
                if (sent) {
                    markEntrySent(context, entry.timestamp)
                }
            }
            netThread.start()
            netThread.join(3500)
        } catch (_: Throwable) {}
    }

    private fun extractLocationFromThrowable(throwable: Throwable): String {
        val appElement = throwable.stackTrace.firstOrNull { it.className.startsWith("com.chiranth7.regibook") }
        return if (appElement != null) {
            "${appElement.fileName ?: "Unknown"}:${appElement.lineNumber} (${appElement.className.substringAfterLast(".")}.${appElement.methodName})"
        } else {
            val first = throwable.stackTrace.firstOrNull()
            if (first != null) "${first.fileName ?: "Unknown"}:${first.lineNumber} (${first.className.substringAfterLast(".")}.${first.methodName})" else ""
        }
    }

    private fun extractCallerLocation(): String {
        return try {
            val elements = Thread.currentThread().stackTrace
            val caller = elements.firstOrNull {
                it.className.startsWith("com.chiranth7.regibook") && !it.className.contains("AppLogManager")
            }
            if (caller != null) {
                "${caller.fileName ?: "Unknown"}:${caller.lineNumber} (${caller.className.substringAfterLast(".")}.${caller.methodName})"
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun getLogs(context: Context): List<AppLogEntry> {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            readLogsInternal(file)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    fun getFormattedLogs(context: Context): String {
        val logs = getLogs(context)
        if (logs.isEmpty()) return "No log entries recorded."
        return logs.joinToString("\n") { it.toFormattedString() }
    }

    fun scheduleRetryOnConnectivity(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val retryRequest = OneTimeWorkRequestBuilder<SyncRetryWorker>()
                .setConstraints(constraints)
                .addTag(RETRY_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                RETRY_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                retryRequest
            )
        } catch (_: Exception) {}
    }

    suspend fun flushPendingLogsToDiscord(context: Context): Int = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, LOG_FILE_NAME)
        val logs = readLogsInternal(file).toMutableList()
        var sentCount = 0
        var hasUpdates = false

        for (entry in logs) {
            if (entry.isError && !entry.isSentToRemote) {
                val payload = buildDiscordPayload(context, entry)
                val success = sendToDiscord(payload)
                if (success) {
                    entry.isSentToRemote = true
                    hasUpdates = true
                    sentCount++
                }
            }
        }

        if (hasUpdates) {
            writeLogsInternal(file, logs)
        }
        sentCount
    }

    fun sendToDiscordSync(jsonPayload: String): Boolean {
        if (isTestEnvironment || DISCORD_WEBHOOK_URL.isBlank()) return false
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(DISCORD_WEBHOOK_URL)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("User-Agent", "RegiBook-Android")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            code in 200..299
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun sendToDiscord(jsonPayload: String): Boolean = withContext(Dispatchers.IO) {
        sendToDiscordSync(jsonPayload)
    }

    private fun markEntrySent(context: Context, timestamp: Long) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val logs = readLogsInternal(file).toMutableList()
            var modified = false
            for (entry in logs) {
                if (entry.timestamp == timestamp) {
                    entry.isSentToRemote = true
                    modified = true
                    break
                }
            }
            if (modified) {
                writeLogsInternal(file, logs)
            }
        } catch (_: Exception) {}
    }

    fun buildDiscordPayload(context: Context, entry: AppLogEntry): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${pInfo.versionName} (${PackageInfoCompat.getLongVersionCode(pInfo)})"
        } catch (_: Exception) {
            "v1.0.7"
        }

        val embed = JSONObject().apply {
            put("title", "🚨 RegiBook Issue: [${entry.tag}]")
            put("color", 15158332)

            val fields = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "📍 Page / Screen")
                    put("value", entry.page.ifBlank { "Unknown" })
                    put("inline", true)
                })
                put(JSONObject().apply {
                    put("name", "📁 Code Location")
                    put("value", entry.location.ifBlank { "N/A" })
                    put("inline", true)
                })
                put(JSONObject().apply {
                    put("name", "📱 Device")
                    put("value", device)
                    put("inline", true)
                })
                put(JSONObject().apply {
                    put("name", "📦 App Version")
                    put("value", appVersion)
                    put("inline", true)
                })
                put(JSONObject().apply {
                    put("name", "⏰ Timestamp")
                    put("value", sdf.format(Date(entry.timestamp)))
                    put("inline", true)
                })
                val truncatedMessage = if (entry.message.length > 900) entry.message.take(900) + "..." else entry.message
                put(JSONObject().apply {
                    put("name", "📝 Error Details")
                    put("value", "```\n$truncatedMessage\n```")
                    put("inline", false)
                })
            }
            put("fields", fields)
        }

        val root = JSONObject().apply {
            put("embeds", JSONArray().apply { put(embed) })
        }

        return root.toString()
    }

    private fun readLogsInternal(file: File): List<AppLogEntry> {
        if (!file.exists()) return emptyList()
        val content = file.readText()
        if (content.isBlank()) return emptyList()
        val jsonArray = JSONArray(content)
        val list = mutableListOf<AppLogEntry>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                AppLogEntry(
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    tag = obj.optString("tag", "General"),
                    message = obj.optString("message", ""),
                    isError = obj.optBoolean("isError", false),
                    page = obj.optString("page", "home"),
                    location = obj.optString("location", ""),
                    isSentToRemote = obj.optBoolean("isSentToRemote", false)
                )
            )
        }
        return list
    }

    private fun writeLogsInternal(file: File, logs: List<AppLogEntry>) {
        val jsonArray = JSONArray()
        for (entry in logs) {
            val obj = JSONObject().apply {
                put("timestamp", entry.timestamp)
                put("tag", entry.tag)
                put("message", entry.message)
                put("isError", entry.isError)
                put("page", entry.page)
                put("location", entry.location)
                put("isSentToRemote", entry.isSentToRemote)
            }
            jsonArray.put(obj)
        }
        file.writeText(jsonArray.toString())
    }
}
