package ru.redbyte.epubreader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import ru.redbyte.epubreader.R

@Composable
private fun prosvLightColorScheme(): ColorScheme = lightColorScheme(
    primary = colorResource(R.color.light_primary),
    onPrimary = colorResource(R.color.light_on_primary),
    primaryContainer = colorResource(R.color.light_primary_container),
    onPrimaryContainer = colorResource(R.color.light_on_primary_container),
    secondary = colorResource(R.color.light_secondary),
    onSecondary = colorResource(R.color.light_on_secondary),
    secondaryContainer = colorResource(R.color.light_secondary_container),
    onSecondaryContainer = colorResource(R.color.light_on_secondary_container),
    tertiary = colorResource(R.color.light_tertiary),
    onTertiary = colorResource(R.color.light_on_tertiary),
    tertiaryContainer = colorResource(R.color.light_tertiary_container),
    onTertiaryContainer = colorResource(R.color.light_on_tertiary_container),
    background = colorResource(R.color.light_background),
    onBackground = colorResource(R.color.light_on_background),
    surface = colorResource(R.color.light_surface),
    onSurface = colorResource(R.color.light_on_surface),
    surfaceVariant = colorResource(R.color.light_surface_variant),
    onSurfaceVariant = colorResource(R.color.light_on_surface_variant),
    outline = colorResource(R.color.light_outline),
    outlineVariant = colorResource(R.color.light_outline_variant),
    error = colorResource(R.color.light_error),
    onError = colorResource(R.color.light_on_error),
    errorContainer = colorResource(R.color.light_error_container),
    onErrorContainer = colorResource(R.color.light_on_error_container),
)

@Composable
private fun prosvDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = colorResource(R.color.dark_primary),
    onPrimary = colorResource(R.color.dark_on_primary),
    primaryContainer = colorResource(R.color.dark_primary_container),
    onPrimaryContainer = colorResource(R.color.dark_on_primary_container),
    secondary = colorResource(R.color.dark_secondary),
    onSecondary = colorResource(R.color.dark_on_secondary),
    secondaryContainer = colorResource(R.color.dark_secondary_container),
    onSecondaryContainer = colorResource(R.color.dark_on_secondary_container),
    tertiary = colorResource(R.color.dark_tertiary),
    onTertiary = colorResource(R.color.dark_on_tertiary),
    tertiaryContainer = colorResource(R.color.dark_tertiary_container),
    onTertiaryContainer = colorResource(R.color.dark_on_tertiary_container),
    background = colorResource(R.color.dark_background),
    onBackground = colorResource(R.color.dark_on_background),
    surface = colorResource(R.color.dark_surface),
    onSurface = colorResource(R.color.dark_on_surface),
    surfaceVariant = colorResource(R.color.dark_surface_variant),
    onSurfaceVariant = colorResource(R.color.dark_on_surface_variant),
    outline = colorResource(R.color.dark_outline),
    outlineVariant = colorResource(R.color.dark_outline_variant),
    error = colorResource(R.color.dark_error),
    onError = colorResource(R.color.dark_on_error),
    errorContainer = colorResource(R.color.dark_error_container),
    onErrorContainer = colorResource(R.color.dark_on_error_container),
)

@Composable
fun EpubReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> prosvDarkColorScheme()
        else -> prosvLightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
