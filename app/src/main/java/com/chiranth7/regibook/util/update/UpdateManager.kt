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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateManager(
    private val context: Context,
    private val githubOwner: String = "Chiranth-Janardhan-moger",
    private val githubRepo: String = "ruled-register"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    suspend fun checkForUpdates(isManualCheck: Boolean = false) {
        _updateState.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "RuledRegister-Android-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 404) {
                            _updateState.value = if (isManualCheck) UpdateState.UpToDate else UpdateState.Idle
                            return@withContext
                        }
                        if (isManualCheck) {
                            _updateState.value = UpdateState.Error("Failed to check for updates (HTTP ${response.code})")
                        } else {
                            _updateState.value = UpdateState.Idle
                        }
                        return@withContext
                    }

                    val responseBody = response.body?.string() ?: throw Exception("Empty response from GitHub")
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
                }
            } catch (e: Exception) {
                _updateState.value = if (isManualCheck) {
                    UpdateState.Error(e.localizedMessage ?: "Network error while checking updates")
                } else {
                    UpdateState.Idle
                }
            }
        }
    }

    suspend fun downloadAndInstall(info: UpdateInfo) {
        _updateState.value = UpdateState.Downloading(info, 0)
        withContext(Dispatchers.IO) {
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updatesDir, info.apkFileName)
                if (apkFile.exists()) apkFile.delete()

                val request = Request.Builder().url(info.apkDownloadUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Download failed (HTTP ${response.code})")
                    }

                    val body = response.body ?: throw Exception("Empty APK response body")
                    val contentLength = body.contentLength()
                    var bytesDownloaded = 0L

                    body.byteStream().use { input ->
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
                }

                _updateState.value = UpdateState.ReadyToInstall(info, apkFile.absolutePath)
                withContext(Dispatchers.Main) {
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Failed to download update")
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
