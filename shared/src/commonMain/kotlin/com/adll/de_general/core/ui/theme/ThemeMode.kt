package com.adll.de_general.core.ui.theme

/**
 * The person's appearance choice, from Settings.
 *
 * A preference, not a colour scheme: [System] only becomes light or dark once the platform's own
 * setting is known, which is what [isDark] is for. Kept pure so the whole decision is testable in
 * `commonTest` without a composition.
 */
enum class ThemeMode(
    /** What is written to disk. Stable on purpose — renaming an entry must not reset anyone. */
    val storageKey: String,
    /** What Settings shows. */
    val label: String,
) {
    System("system", "System"),
    Light("light", "Light"),
    Dark("dark", "Dark");

    /** Resolves this choice against the platform's dark-mode setting. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        /**
         * Reads a stored key back. Anything unrecognised — null, blank, a value from some future
         * version — is [System], so a bad file can never lock someone into a theme.
         */
        fun fromStorageKey(key: String?): ThemeMode {
            val trimmed = key?.trim() ?: return System
            return entries.firstOrNull { it.storageKey == trimmed } ?: System
        }
    }
}
