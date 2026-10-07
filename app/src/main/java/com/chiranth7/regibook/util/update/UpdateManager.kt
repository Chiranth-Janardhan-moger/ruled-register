package com.chiranth7.regibook.util.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.chiranth7.regibook.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

class UpdateManager(
    private val context: Context,
    private val githubOwner: String = "Chiranth-Janardhan-moger",
    private val githubRepo: String = "ruled-register"
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private fun openConnectionWithRedirects(urlString: String, maxRedirects: Int = 5): HttpURLConnection {
        var url = URI(urlString).toURL()
        var redirects = 0
        while (redirects < maxRedirects) {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 60000
                setRequestProperty("User-Agent", "RuledRegister-Android-App")
                instanceFollowRedirects = false
            }
            val status = conn.responseCode
            if (status in 300..399) {
                val location = conn.getHeaderField("Location") ?: throw Exception("Redirect missing Location header")
                conn.disconnect()
                url = URI(location).toURL()
                redirects++
            } else {
                return conn
            }
        }
        throw Exception("Too many redirects")
    }

    suspend fun checkForUpdates(isManualCheck: Boolean = false) {
        _updateState.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val urlString = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
                connection = openConnectionWithRedirects(urlString).apply {
                    setRequestProperty("Accept", "application/vnd.github+json")
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    if (responseCode == 404) {
                        _updateState.value = if (isManualCheck) UpdateState.UpToDate else UpdateState.Idle
                        return@withContext
                    }
                    if (isManualCheck) {
                        _updateState.value = UpdateState.Error("Failed to check for updates (HTTP $responseCode)")
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                    return@withContext
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseBody)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val body = json.optString("body", "Bug fixes and performance improvements")
                val assets = json.optJSONArray("assets")

                var apkUrl: String? = null
                var apkName = "ruled-register-update.apk"

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            apkName = name
                            break
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

                if (apkUrl != null && isNewerVersion(tagName, currentVersion)) {
                    _updateState.value = UpdateState.UpdateAvailable(
                        UpdateInfo(
                            versionName = tagName,
                            releaseNotes = body,
                            apkDownloadUrl = apkUrl,
                            apkFileName = apkName
                        )
                    )
                } else {
                    _updateState.value = if (isManualCheck) UpdateState.UpToDate else UpdateState.Idle
                }
            } catch (e: Exception) {
                _updateState.value = if (isManualCheck) {
                    UpdateState.Error(e.localizedMessage ?: "Network error while checking updates")
                } else {
                    UpdateState.Idle
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    suspend fun downloadAndInstall(info: UpdateInfo) {
        _updateState.value = UpdateState.Downloading(info, 0)
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updatesDir, info.apkFileName)
                if (apkFile.exists()) apkFile.delete()

                connection = openConnectionWithRedirects(info.apkDownloadUrl)
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw Exception("Download failed (HTTP $responseCode)")
                }

                val contentLength = connection.contentLengthLong
                var bytesDownloaded = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesDownloaded += read
                            if (contentLength > 0) {
                                val percent = ((bytesDownloaded * 100) / contentLength).toInt()
                                _updateState.value = UpdateState.Downloading(info, percent)
                            }
                        }
                        output.flush()
                    }
                }

                _updateState.value = UpdateState.ReadyToInstall(info, apkFile.absolutePath)
                withContext(Dispatchers.Main) {
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Failed to download update")
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun installApk(file: File) {
        if (!file.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        if (remote == current) return false
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
