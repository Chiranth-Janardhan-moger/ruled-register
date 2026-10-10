package com.chiranth7.regibook.features.pigmi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pigmi_accounts",
    indices = [
        Index(value = ["srNo"]),
        Index(value = ["name"])
    ]
)
data class PigmiAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val srNo: Int,
    val name: String,
    val phoneNumber: String = "",
    val address: String = "",
    val accountNumber: String = "",
    val dailyAmount: String = "",
    val kannadaName: String = ""
) {
    fun getDisplayName(currentLanguage: String): String {
        return if (currentLanguage == com.chiranth7.regibook.util.SettingsManager.LANG_KANNADA) {
            if (kannadaName.isNotBlank()) kannadaName else com.chiranth7.regibook.util.KannadaNameHelper.formatDisplayName(name, currentLanguage)
        } else {
            name
        }
    }
}
