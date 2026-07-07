package com.example.ui.theme

import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Mizan Al-Dar Color Palette - Now styled with the premium Violet & Neon Cyan "الدفتر الذكي" Palette
val EmeraldPrimary = Color(0xFF4B36A2)      // Premium Glowing Violet/Purple (#4B36A2)
val EmeraldLight = Color(0xFF8C7CFF)        // Lighter Purple/Lavender for beautiful gradients (#8C7CFF)
val CoralAccent = Color(0xFF00B2FE)         // Neon Cyan / Electric Blue Accent (#00B2FE)

val EmeraldDark = Color(0xFF6756AB)         // Desaturated for dark mode
val CoralDark = Color(0xFF4DBDF0)           // Desaturated for dark mode
val IvoryBackground = Color(0xFFF0F3FC)     // Soft modern lavender-tinted background (Eye safe) (#F0F3FC)

val SoftRed = Color(0xFFE05252)            // Warm Soft Red (Incomplete or Expense)
val SoftGreen = Color(0xFF3CD070)          // Vibrant Soft Green (Complete or Income)

val DarkBackground = Color(0xFF121212)     // Deep Charcoal background (#121212)
val DarkSurface = Color(0xFF1E1E1E)        // Deep charcoal card surface (#1E1E1E)
val LightSurface = Color(0xFFFFFFFF)       // Clean white card surface

val TextPrimaryDark = Color(0xFFE1E1E1)     // Soft White primary text (#E1E1E1)
val TextSecondaryDark = Color(0xFFB0B3B8)   // Soft gray secondary text (#B0B3B8)
val TextPrimaryLight = Color(0xFF1E1A3E)    // Deep indigo-slate primary text (#1E1A3E)
val TextSecondaryLight = Color(0xFF5C58A5)  // Muted purple-slate secondary text

val BorderDark = Color(0xFF2C2C2C)          // Soft dark gray border for depth (#2C2C2C)
val BorderLight = Color(0xFFE2E8F0)         // Light border

val PrimaryGradient = Brush.linearGradient(
    colors = listOf(EmeraldPrimary, EmeraldLight)
)

val CoralGradient = Brush.linearGradient(
    colors = listOf(CoralAccent, Color(0xFF0284C7))
)

