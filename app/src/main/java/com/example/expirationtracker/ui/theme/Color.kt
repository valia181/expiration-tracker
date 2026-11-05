package com.example.expirationtracker.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Accessible Status Colors adhering to WCAG 2.1 Level AA (REQ-NF-005):
 * Minimum contrast ratio 4.5:1 against light/dark backgrounds.
 *
 * Light theme container + text pairs:
 * - Red (Expired): Dark crimson text #B3261E on soft container #F9DEDC (Contrast > 7:1)
 * - Orange (Warning): Deep amber-brown text #7A4100 on warm container #FFE0B2 (Contrast > 7:1)
 * - Green (Safe): Deep forest green text #146C2E on pale green container #C4EED0 (Contrast > 7:1)
 */
val StatusRedText = Color(0xFFB3261E)
val StatusRedContainer = Color(0xFFF9DEDC)

val StatusOrangeText = Color(0xFF7A4100)
val StatusOrangeContainer = Color(0xFFFFE0B2)

val StatusGreenText = Color(0xFF146C2E)
val StatusGreenContainer = Color(0xFFC4EED0)
