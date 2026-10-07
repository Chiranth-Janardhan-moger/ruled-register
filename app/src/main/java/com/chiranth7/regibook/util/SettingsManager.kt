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
        prefs.edit().putString(KEY_LANGUAGE, validCode).apply()
        _currentLanguage.value = validCode
    }

    fun setActiveRegisterType(type: RegisterType) {
        prefs.edit().putString(KEY_REGISTER_TYPE, type.name).apply()
        _activeRegisterType.value = type
    }

    // Agent Number for LIC profile
    private val _agentNumber = MutableStateFlow(
        prefs.getString(KEY_AGENT_NUMBER, "") ?: ""
    )
    val agentNumber: StateFlow<String> = _agentNumber.asStateFlow()

    fun setAgentNumber(number: String) {
        val trimmed = number.trim()
        prefs.edit().putString(KEY_AGENT_NUMBER, trimmed).apply()
        _agentNumber.value = trimmed
    }

    fun getLocale(): Locale = Locale(_currentLanguage.value)

    companion object {
        private const val PREFS_NAME = "register_book_settings"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_REGISTER_TYPE = "selected_register_type"
        private const val KEY_AGENT_NUMBER = "lic_agent_number"

        const val LANG_ENGLISH = "en"
        const val LANG_KANNADA = "kn"
    }
}
