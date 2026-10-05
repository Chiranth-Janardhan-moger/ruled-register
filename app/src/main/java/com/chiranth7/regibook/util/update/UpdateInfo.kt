package com.chiranth7.regibook.util.update

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progressPercent: Int) : UpdateState
    data class ReadyToInstall(val info: UpdateInfo, val apkPath: String) : UpdateState
    data class Error(val message: String) : UpdateState
    data object UpToDate : UpdateState
}
