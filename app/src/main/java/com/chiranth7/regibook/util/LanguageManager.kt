package com.chiranth7.regibook.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LanguageManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(
        prefs.getString(KEY_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
    )
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(languageCode: String) {
        val validCode = if (languageCode == LANG_KANNADA) LANG_KANNADA else LANG_ENGLISH
        prefs.edit().putString(KEY_LANGUAGE, validCode).apply()
        _currentLanguage.value = validCode
    }

    fun getLocale(): Locale {
        return Locale(_currentLanguage.value)
    }

    companion object {
        private const val PREFS_NAME = "register_book_settings"
        private const val KEY_LANGUAGE = "selected_language"
        const val LANG_ENGLISH = "en"
        const val LANG_KANNADA = "kn"
    }
}
