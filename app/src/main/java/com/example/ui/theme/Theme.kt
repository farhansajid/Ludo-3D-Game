package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = BrushedBrass,
  onPrimary = ObsidianNight,
  primaryContainer = BasaltElevated,
  onPrimaryContainer = WarmIvoryText,
  secondary = TerracottaRed,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF422622),
  onSecondaryContainer = TerracottaSoft,
  tertiary = EucalyptusSage,
  onTertiary = Color.White,
  background = ObsidianNight,
  onBackground = WarmIvoryText,
  surface = BasaltSurface,
  onSurface = WarmIvoryText,
  surfaceVariant = BasaltElevated,
  onSurfaceVariant = MutedStoneText,
  outline = StoneBorderDark
)

private val LightColorScheme = lightColorScheme(
  primary = Obsidianink,
  onPrimary = AlabasterLinen,
  primaryContainer = WarmSandCard,
  onPrimaryContainer = Obsidianink,
  secondary = TerracottaRed,
  onSecondary = Color.White,
  secondaryContainer = TerracottaSoft,
  onSecondaryContainer = TerracottaRedDark,
  tertiary = EucalyptusSage,
  onTertiary = Color.White,
  background = AlabasterLinen,
  onBackground = Obsidianink,
  surface = TravertineStone,
  onSurface = Obsidianink,
  surfaceVariant = WarmSandCard,
  onSurfaceVariant = CharcoalMuted,
  outline = StoneBorderLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
