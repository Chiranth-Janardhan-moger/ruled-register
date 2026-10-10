package com.chiranth7.regibook.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class RegisterType {
    PIGMI,
    LIC
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Language: English (default) or Kannada
    private val _currentLanguage = MutableStateFlow(
        prefs.getString(KEY_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
    )
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Active Register: Pigmi (default) or LIC
    private val _activeRegisterType = MutableStateFlow(
        try {
            RegisterType.valueOf(
                prefs.getString(KEY_REGISTER_TYPE, RegisterType.PIGMI.name) ?: RegisterType.PIGMI.name
            )
        } catch (e: Exception) {
            RegisterType.PIGMI
        }
    )
    val activeRegisterType: StateFlow<RegisterType> = _activeRegisterType.asStateFlow()

    fun setLanguage(languageCode: String) {
        val validCode = if (languageCode == LANG_KANNADA) LANG_KANNADA else LANG_ENGLISH
        prefs.edit().putString(KEY_LANGUAGE, validCode).commit()
        _currentLanguage.value = validCode
    }

    fun setActiveRegisterType(type: RegisterType) {
        prefs.edit().putString(KEY_REGISTER_TYPE, type.name).apply()
        _activeRegisterType.value = type
    }

    // Agent Number for LIC profile
    private val _agentNumber = MutableStateFlow(
        prefs.getString(KEY_AGENT_NUMBER, DEFAULT_AGENT_CODE) ?: DEFAULT_AGENT_CODE
    )
    val agentNumber: StateFlow<String> = _agentNumber.asStateFlow()

    fun setAgentNumber(number: String) {
        val trimmed = number.trim()
        prefs.edit().putString(KEY_AGENT_NUMBER, trimmed).apply()
        _agentNumber.value = trimmed
    }

    // Policy Cloud Sync URL (e.g. GitHub Gist raw URL or online JSON)
    private val _policySyncUrl = MutableStateFlow(
        prefs.getString(KEY_POLICY_SYNC_URL, DEFAULT_POLICY_SYNC_URL) ?: DEFAULT_POLICY_SYNC_URL
    )
    val policySyncUrl: StateFlow<String> = _policySyncUrl.asStateFlow()

    fun setPolicySyncUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_POLICY_SYNC_URL, trimmed).apply()
        _policySyncUrl.value = trimmed
    }

    // Pigmi Cloud Sync URL (e.g. GitHub raw URL or online JSON)
    private val _pigmiSyncUrl = MutableStateFlow(
        prefs.getString(KEY_PIGMI_SYNC_URL, DEFAULT_PIGMI_SYNC_URL) ?: DEFAULT_PIGMI_SYNC_URL
    )
    val pigmiSyncUrl: StateFlow<String> = _pigmiSyncUrl.asStateFlow()

    fun setPigmiSyncUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_PIGMI_SYNC_URL, trimmed).apply()
        _pigmiSyncUrl.value = trimmed
    }

    // Last Sync Timestamp in millis
    private val _lastSyncTimestamp = MutableStateFlow(
        prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    )
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
        _lastSyncTimestamp.value = timestamp
    }

    // Font Scale: 0.85f to 1.45f
    private val _fontScale = MutableStateFlow(
        prefs.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE)
    )
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        val rounded = Math.round(clamped * 100f) / 100f
        prefs.edit().putFloat(KEY_FONT_SCALE, rounded).apply()
        _fontScale.value = rounded
    }

    fun increaseFontScale() {
        setFontScale(_fontScale.value + FONT_SCALE_STEP)
    }

    fun decreaseFontScale() {
        setFontScale(_fontScale.value - FONT_SCALE_STEP)
    }

    fun getLocale(): Locale = Locale.forLanguageTag(_currentLanguage.value)

    // Policy Notified Status (Silences reminders for current due cycle)
    fun getNotifiedPolicyKey(policyNumber: String, dueDate: String): String =
        "${policyNumber.trim()}_${dueDate.trim()}"

    fun isPolicyNotified(policyNumber: String, dueDate: String): Boolean {
        if (policyNumber.isBlank() || dueDate.isBlank()) return false
        val key = getNotifiedPolicyKey(policyNumber, dueDate)
        val set = prefs.getStringSet(KEY_NOTIFIED_POLICIES, emptySet()) ?: emptySet()
        return set.contains(key)
    }

    fun setPolicyNotified(policyNumber: String, dueDate: String, notified: Boolean) {
        if (policyNumber.isBlank() || dueDate.isBlank()) return
        val key = getNotifiedPolicyKey(policyNumber, dueDate)
        val currentSet = prefs.getStringSet(KEY_NOTIFIED_POLICIES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (notified) {
            currentSet.add(key)
        } else {
            currentSet.remove(key)
        }
        prefs.edit().putStringSet(KEY_NOTIFIED_POLICIES, currentSet).apply()
    }

    companion object {
        private const val PREFS_NAME = "register_book_settings"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_REGISTER_TYPE = "selected_register_type"
        private const val KEY_AGENT_NUMBER = "lic_agent_number"
        private const val KEY_POLICY_SYNC_URL = "lic_policy_sync_url"
        private const val KEY_PIGMI_SYNC_URL = "pigmi_sync_url"
        private const val KEY_LAST_SYNC_TIMESTAMP = "lic_last_sync_timestamp"
        private const val KEY_FONT_SCALE = "app_font_scale"
        private const val KEY_NOTIFIED_POLICIES = "notified_policy_keys"

        const val DEFAULT_AGENT_CODE = "LIC0246463V"
        const val DEFAULT_POLICY_SYNC_URL = ""
        const val DEFAULT_PIGMI_SYNC_URL = ""

        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.45f
        const val FONT_SCALE_STEP = 0.10f

        const val LANG_ENGLISH = "en"
        const val LANG_KANNADA = "kn"
    }
}
