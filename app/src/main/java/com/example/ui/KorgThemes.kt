package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Pa4xDarkColors = darkColorScheme(
    primary = Color(0xFFFBBF24),
    secondary = Color(0xFF38BDF8),
    tertiary = Color(0xFF10B981),
    background = Color(0xFF101318),
    surface = Color(0xFF181D26),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun Pa4xAppTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colors = if (darkTheme) Pa4xDarkColors else Pa4xDarkColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

object KorgPa4xTheme {
    val ChassisDarkGunmetal = Color(0xFF101318)
    val ChassisBrushedMid = Color(0xFF181D26)
    val ChassisBorder = Color(0xFF333D4D)
    val KorgAmberGold = Color(0xFFFBBF24)
    val KorgCyanNeon = Color(0xFF38BDF8)
    val KorgLedGreen = Color(0xFF10B981)
    val KorgLedRed = Color(0xFFEF4444)
    val LcdNavyPanel = Color(0xFF0F1A30)
    val LcdBorderBlue = Color(0xFF1E3A68)
    val ButtonBodyTop = Color(0xFF2A3342)
    val ButtonBodyBottom = Color(0xFF171D27)
    val ButtonBorderLight = Color(0xFF475569)
    val ButtonPressedTop = Color(0xFF141A24)
    val ButtonPressedBottom = Color(0xFF1F2633)
    val IvoryWhiteKeyBorder = Color(0xFF94A3B8)
    val EbonyBlackTop = Color(0xFF2C2D32)
    val EbonyBlackBottom = Color(0xFF0C0D10)
    val EbonyBlackHighlight = Color(0xFF484B55)
    val EbonyBlackBorder = Color(0xFF000000)
}
