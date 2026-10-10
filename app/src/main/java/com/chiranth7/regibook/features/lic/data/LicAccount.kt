package com.chiranth7.regibook.features.lic.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lic_accounts",
    indices = [
        Index(value = ["name"]),
        Index(value = ["policyNumber"]),
        Index(value = ["policyName"])
    ]
)
data class LicAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val policyNumber: String,
    val policyName: String = "",
    val totalYears: String = "",
    val phoneNumber: String = "",
    val lastPaymentDate: String = "",
    val nextPaymentDate: String = "",
    val premiumAmount: String = "",
    val address: String = "",
    val commencementDate: String = "",
    val lastPremiumDate: String = "",
    val maturityDate: String = "",
    val kannadaName: String = ""
) {
    fun getDisplayName(currentLanguage: String): String {
        return if (currentLanguage == com.chiranth7.regibook.util.SettingsManager.LANG_KANNADA) {
            if (kannadaName.isNotBlank()) kannadaName else com.chiranth7.regibook.util.KannadaNameHelper.formatDisplayName(name, currentLanguage)
        } else {
            name
        }
    }

    fun getDisplayPolicyName(currentLanguage: String): String {
        // Strip "LIC'S", "LICS", or "LIC" from policy names (e.g., "736 - LIC'S JEEVAN LABH PLAN" -> "736 - JEEVAN LABH PLAN")
        val cleaned = policyName
            .replace(Regex("(?i)\\bLIC'?S\\s*"), "")
            .replace(Regex("\\s+-\\s+"), " - ")
            .trim()

        return if (currentLanguage == com.chiranth7.regibook.util.SettingsManager.LANG_KANNADA) {
            com.chiranth7.regibook.util.KannadaNameHelper.formatDisplayName(cleaned, currentLanguage)
        } else {
            cleaned
        }
    }
}
