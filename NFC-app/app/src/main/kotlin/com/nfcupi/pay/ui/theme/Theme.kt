package com.nfcupi.pay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TapmeColorScheme = darkColorScheme(
    primary              = TapmeOrange,
    onPrimary            = TapmeBackground,
    primaryContainer     = TapmeOrangeBg,
    onPrimaryContainer   = TapmeOrange,
    secondary            = TapmeMuted,
    onSecondary          = TapmeText,
    secondaryContainer   = TapmeSurface2,
    onSecondaryContainer = TapmeText,
    tertiary             = TapmeMuted2,
    onTertiary           = TapmeText,
    tertiaryContainer    = TapmeBorder,
    onTertiaryContainer  = TapmeText,
    background           = TapmeBackground,
    onBackground         = TapmeText,
    surface              = TapmeSurface,
    onSurface            = TapmeText,
    surfaceVariant       = TapmeSurface2,
    onSurfaceVariant     = TapmeMuted,
    outline              = TapmeBorder,
    outlineVariant       = TapmeMuted3,
    error                = TapmeError,
    onError              = TapmeBackground,
    errorContainer       = TapmeError.copy(alpha = 0.12f),
    onErrorContainer     = TapmeError,
)

@Composable
fun NfcUpiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TapmeColorScheme,
        typography = Typography,
        content = content,
    )
}
