package com.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun customTextFieldColors(isDark: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF78350F).invertIfDark(isDark),
    unfocusedTextColor = Color(0xFF78350F).invertIfDark(isDark),
    focusedBorderColor = Color(0xFF78350F).invertIfDark(isDark),
    unfocusedBorderColor = Color(0xFF78350F).copy(alpha = 0.4f).invertIfDark(isDark),
    focusedLabelColor = Color(0xFF78350F).invertIfDark(isDark),
    unfocusedLabelColor = Color(0xFF78350F).copy(alpha = 0.7f).invertIfDark(isDark),
    cursorColor = Color(0xFF78350F).invertIfDark(isDark)
)

@Composable
fun resolveIsDarkTheme(themeMode: String): Boolean {
    return when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
}

/**
 * Inverts an RGB color if [isDark] is true.
 * Light theme color inversion formula:
 * R' = 1 - R, G' = 1 - G, B' = 1 - B, preserving alpha.
 */
fun Color.invertIfDark(isDark: Boolean): Color {
    return if (isDark) {
        Color(
            red = (1f - this.red).coerceIn(0f, 1f),
            green = (1f - this.green).coerceIn(0f, 1f),
            blue = (1f - this.blue).coerceIn(0f, 1f),
            alpha = this.alpha
        )
    } else {
        this
    }
}
