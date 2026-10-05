package com.chiranth7.regibook.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class MarginLineColor {
    RED,
    BLACK
}

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

    // Theme: Default to LIGHT as requested!
    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(
                prefs.getString(KEY_THEME, AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name
            )
        } catch (e: Exception) {
            AppThemeMode.LIGHT
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Margin Line Color: Red (default) or Black
    private val _marginLineColor = MutableStateFlow(
        try {
            MarginLineColor.valueOf(
                prefs.getString(KEY_MARGIN_COLOR, MarginLineColor.RED.name) ?: MarginLineColor.RED.name
            )
        } catch (e: Exception) {
            MarginLineColor.RED
        }
    )
    val marginLineColor: StateFlow<MarginLineColor> = _marginLineColor.asStateFlow()

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

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setMarginLineColor(color: MarginLineColor) {
        prefs.edit().putString(KEY_MARGIN_COLOR, color.name).apply()
        _marginLineColor.value = color
    }

    fun setActiveRegisterType(type: RegisterType) {
        prefs.edit().putString(KEY_REGISTER_TYPE, type.name).apply()
        _activeRegisterType.value = type
    }

    fun getLocale(): Locale = Locale(_currentLanguage.value)

    companion object {
        private const val PREFS_NAME = "register_book_settings"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_MARGIN_COLOR = "selected_margin_color"
        private const val KEY_REGISTER_TYPE = "selected_register_type"

        const val LANG_ENGLISH = "en"
        const val LANG_KANNADA = "kn"
    }
}
