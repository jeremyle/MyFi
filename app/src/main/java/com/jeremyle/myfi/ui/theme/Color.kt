package com.jeremyle.myfi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Semantic: portfolio gain/loss indicators used throughout the app
val GainGreen = Color(0xFF00C805)
val LossOrange = Color(0xFFFF6D00)

// Semantic: inactive/unselected tab label
val TabUnselected = Color(0xFF666666)

// Semantic: secondary text (shares count, chevron, sub-labels)
val TextSecondary = Color(0xFF888888)

@Composable
fun selectedTextColor(selected: Boolean): Color =
    if (selected) MaterialTheme.colorScheme.onBackground else TabUnselected

@Composable
fun dividerColor(): Color = MaterialTheme.colorScheme.outlineVariant