package com.chiranth7.regibook.util

object KannadaNameHelper {

    // Common names, surnames, and policy names mapping
    private val nameDictionary = mapOf(
        "chiranth" to "ಚಿರಂತ",
        "chirant" to "ಚಿರಂತ",
        "janardhan" to "ಜನಾರ್ದನ",
        "janardhana" to "ಜನಾರ್ದನ",
        "moger" to "ಮೊಗೇರ",
        "chiranth janardhan moger" to "ಚಿರಂತ ಜನಾರ್ದನ ಮೊಗೇರ",
        "kiran" to "ಕಿರಣ್",
        "kumar" to "ಕುಮಾರ್",
        "kiran kumar" to "ಕಿರಣ್ ಕುಮಾರ್",
        "pooja" to "ಪೂಜಾ",
        "sharma" to "ಶರ್ಮ",
        "pooja sharma" to "ಪೂಜಾ ಶರ್ಮ",
        "suresh" to "ಸುರೇಶ್",
        "gowda" to "ಗೌಡ",
        "suresh gowda" to "ಸುರೇಶ್ ಗೌಡ",
        "ramesh" to "ರಮೇಶ್",
        "ramesh gowda" to "ರಮೇಶ್ ಗೌಡ",
        "manjunath" to "ಮಂಜುನಾಥ್",
        "ganesh" to "ಗಣೇಶ್",
        "venkatesh" to "ವೆಂಕಟೇಶ್",
        "prashanth" to "ಪ್ರಶಾಂತ್",
        "santosh" to "ಸಂತೋಷ್",
        "anand" to "ಆನಂದ್",
        "shetty" to "ಶೆಟ್ಟಿ",
        "bhat" to "ಭಟ್",
        "hegde" to "ಹೆಗಡೆ",
        "naik" to "ನಾಯ್ಕ್",
        "patil" to "ಪಾಟೀಲ್",
        "rao" to "ರಾವ್",
        "reddy" to "ರೆಡ್ಡಿ",
        "jeevan labh" to "ಜೀವನ್ ಲಾಭ್",
        "jeevan umang" to "ಜೀವನ್ ಉಮಂಗ್",
        "jeevan anand" to "ಜೀವನ್ ಆನಂದ್",
        "lic" to "ಎಲ್.ಐ.ಸಿ",
        "lic's" to "ಎಲ್.ಐ.ಸಿ",
        "lic's jeevan labh plan" to "ಎಲ್.ಐ.ಸಿ ಜೀವನ್ ಲಾಭ್ ಯೋಜನೆ",
        "jeevan labh plan" to "ಜೀವನ್ ಲಾಭ್ ಯೋಜನೆ",
        "plan" to "ಯೋಜನೆ"
    )

    // Consonant mapping
    private val consonantMap = listOf(
        "chh" to "ಛ", "kh" to "ಖ", "gh" to "ಘ", "ch" to "ಚ", "jh" to "ಝ",
        "th" to "ತ", "dh" to "ದ", "ph" to "ಫ", "bh" to "ಭ", "sh" to "ಶ",
        "k" to "ಕ", "g" to "ಗ", "c" to "ಕ", "j" to "ಜ", "t" to "ತ",
        "d" to "ದ", "n" to "ನ", "p" to "ಪ", "f" to "ಫ", "b" to "ಬ",
        "m" to "ಮ", "y" to "ಯ", "r" to "ರ", "l" to "ಲ", "v" to "ವ",
        "w" to "ವ", "s" to "ಸ", "h" to "ಹ", "z" to "ಜ"
    )

    // Dependent vowel signs (matras)
    private val matraMap = listOf(
        "aa" to "ಾ", "ee" to "ೀ", "oo" to "ೂ", "ai" to "ೈ", "au" to "ೌ",
        "ou" to "ೌ", "a" to "", "i" to "ಿ", "u" to "ು", "e" to "ೆ", "o" to "ೊ"
    )

    // Independent initial vowels
    private val initialVowelMap = listOf(
        "aa" to "ಆ", "ee" to "ಈ", "oo" to "ಊ", "ai" to "ಐ", "au" to "ಔ",
        "ou" to "ಔ", "a" to "ಅ", "i" to "ಇ", "u" to "ಉ", "e" to "ಎ", "o" to "ಒ"
    )

    /**
     * Converts an English name/text to Kannada if Kannada is the active language.
     * Preserves numbers, currency signs, and punctuation in original ASCII format.
     */
    fun formatDisplayName(text: String, currentLanguage: String): String {
        if (text.isBlank() || currentLanguage != SettingsManager.LANG_KANNADA) {
            return text
        }

        // If the text is already containing Kannada unicode characters, keep as is
        if (text.any { it in '\u0C80'..'\u0CFF' }) {
            return text
        }

        val lower = text.trim().lowercase()
        nameDictionary[lower]?.let { return it }

        // Tokenize and translate word by word while preserving numbers & symbols
        val tokens = text.split(Regex("(?<=[\\s\\-\\.,/()]+)|(?=[\\s\\-\\.,/()]+)"))
        val sb = StringBuilder()

        for (token in tokens) {
            if (token.isBlank() || token.matches(Regex("[\\s\\-\\.,/()0-9₹+]+"))) {
                sb.append(token)
            } else {
                val tokenLower = token.lowercase()
                val mapped = nameDictionary[tokenLower]
                if (mapped != null) {
                    sb.append(mapped)
                } else {
                    sb.append(transliterateWord(token))
                }
            }
        }

        return sb.toString()
    }

    private fun transliterateWord(word: String): String {
        val lower = word.lowercase()
        val result = StringBuilder()
        var i = 0

        while (i < lower.length) {
            val char = lower[i]

            // If digit or non-letter, retain as-is (keeps numbers in English)
            if (!char.isLetter()) {
                result.append(char)
                i++
                continue
            }

            // Check if initial independent vowel
            if (i == 0 || result.isEmpty() || !result.last().isLetter()) {
                var foundVowel = false
                for ((vKey, vVal) in initialVowelMap) {
                    if (lower.startsWith(vKey, i)) {
                        result.append(vVal)
                        i += vKey.length
                        foundVowel = true
                        break
                    }
                }
                if (foundVowel) continue
            }

            // Match consonant
            var foundConsonant: String? = null
            var consKeyLen = 0
            for ((cKey, cVal) in consonantMap) {
                if (lower.startsWith(cKey, i)) {
                    foundConsonant = cVal
                    consKeyLen = cKey.length
                    break
                }
            }

            if (foundConsonant != null) {
                i += consKeyLen
                // Check if following vowel exists
                var foundMatra: String? = null
                var matraKeyLen = 0
                for ((mKey, mVal) in matraMap) {
                    if (lower.startsWith(mKey, i)) {
                        foundMatra = mVal
                        matraKeyLen = mKey.length
                        break
                    }
                }

                if (foundMatra != null) {
                    result.append(foundConsonant).append(foundMatra)
                    i += matraKeyLen
                } else {
                    // Check if at the end of the word or before another consonant -> add halant (್)
                    if (i >= lower.length || lower[i] != 'a') {
                        // Word ending consonants in Kannada usually carry halant (e.g., -n, -r, -sh)
                        result.append(foundConsonant).append("್")
                    } else {
                        result.append(foundConsonant)
                    }
                }
            } else {
                // Single unhandled letter fallback
                result.append(char)
                i++
            }
        }

        // Clean up double virama / halants if any
        return result.toString().replace("್್", "್")
    }
}
