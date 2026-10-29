package com.mdhasan.htube.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.mdhasan.htube.core.designsystem.HTubeTheme
import com.mdhasan.htube.core.model.AppSettings
import com.mdhasan.htube.core.model.ThemeMode

@Composable
fun AppSettings.isDark(): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** The app theme as configured in Settings → Appearance. */
@Composable
fun AppTheme(settings: AppSettings, content: @Composable () -> Unit) {
    HTubeTheme(
        darkTheme = settings.isDark(),
        accent = settings.accent,
        amoled = settings.amoled,
        dynamicColor = settings.dynamicColor,
        content = content,
    )
}
