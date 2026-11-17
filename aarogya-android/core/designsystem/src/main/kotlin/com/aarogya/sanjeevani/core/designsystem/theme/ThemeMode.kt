package com.aarogya.sanjeevani.core.designsystem.theme

/**
 * The user's theme preference, persisted by whatever screen exposes it
 * (Profile) and read back wherever [AarogyaTheme] is installed. [System]
 * is the default -- an explicit choice is only made once the user
 * overrides it.
 */
enum class ThemeMode {
    System,
    Light,
    Dark,
}
