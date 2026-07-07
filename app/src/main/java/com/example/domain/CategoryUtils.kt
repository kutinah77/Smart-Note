package com.example.domain

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.R

fun extractEmoji(category: String, defaultEmoji: String, context: Context? = null): String {
    val cat = category.lowercase()
    
    val flour = context?.getString(R.string.category_flour) ?: "دقيق"
    val gas = context?.getString(R.string.category_gas) ?: "غاز"
    val electricity = context?.getString(R.string.category_electricity) ?: "كهرباء"
    val water = context?.getString(R.string.category_water) ?: "ماء"
    val milk = context?.getString(R.string.category_milk) ?: "حليب"
    val diapers = context?.getString(R.string.category_diapers) ?: "حفاظ"
    val sugar = context?.getString(R.string.category_sugar) ?: "سكر"
    val tea = context?.getString(R.string.category_tea) ?: "شاي"
    val internet = context?.getString(R.string.category_internet) ?: "إنترنت"
    val school = context?.getString(R.string.category_school) ?: "مدرس"
    val savings = context?.getString(R.string.category_savings) ?: "ادخار"
    val emergency = context?.getString(R.string.category_emergency) ?: "طوارئ"
    val medical = context?.getString(R.string.category_medical) ?: "علاج"
    val furniture = context?.getString(R.string.category_furniture) ?: "أثاث"

    return when {
        cat.contains(flour) || cat.contains("🌾") -> "🌾"
        cat.contains(gas) || cat.contains("🔥") -> "🔥"
        cat.contains(electricity) || cat.contains("⚡") -> "⚡"
        cat.contains(water) || cat.contains("💧") -> "💧"
        cat.contains(milk) || cat.contains("🍼") -> "🍼"
        cat.contains(diapers) || cat.contains("👶") -> "👶"
        cat.contains(sugar) || cat.contains("🍬") -> "🍬"
        cat.contains(tea) || cat.contains("☕") -> "☕"
        cat.contains("نت") || cat.contains("رصيد") || cat.contains("🌐") || cat.contains(internet) -> "🌐"
        cat.contains(school) || cat.contains("🎒") -> "🎒"
        cat.contains(savings) || cat.contains("🏦") -> "🏦"
        cat.contains(emergency) || cat.contains("🚨") -> "🚨"
        cat.contains(medical) || cat.contains("💊") -> "💊"
        cat.contains(furniture) || cat.contains("🛋️") -> "🛋️"
        else -> {
            // Check if there is already an emoji in the string
            val emojiRegex = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+".toRegex()
            val match = emojiRegex.find(category)
            match?.value ?: defaultEmoji
        }
    }
}

fun getEmojiBgColor(emoji: String): Color {
    return when (emoji) {
        "🌾" -> Color(0xFFFEF3C7) // amber-100 (🌾)
        "🍬", "🍭" -> Color(0xFFFCE7F3) // pink-100 (🍬)
        "☕" -> Color(0xFFEFEFEF) // gray-100 (☕)
        "🔥" -> Color(0xFFFEE2E2) // red-100 (🔥)
        "⚡" -> Color(0xFFFEF9C3) // yellow-100 (⚡)
        "💧" -> Color(0xFFDBEAFE) // blue-100 (💧)
        "🚀", "🌐" -> Color(0xFFE0F2FE) // sky-100 (🌐)
        "🍼", "👶" -> Color(0xFFF3E8FF) // purple-100 (👶, 🍼)
        "🎒" -> Color(0xFFE0F2FE) // sky-100 (🎒)
        "🏦" -> Color(0xFFD1FAE5) // emerald-100 (🏦)
        "🚨" -> Color(0xFFFFE4E6) // rose-100 (🚨)
        "💊" -> Color(0xFFFCE7F3) // pink-100 (💊)
        "🛋️" -> Color(0xFFF3F4F6) // gray-100 (🛋️)
        "💰" -> Color(0xFFECFDF5) // green-50
        else -> Color(0xFFF1F5F9) // slate-100
    }
}

fun getAuditLogGroupDate(timestampMs: Long, context: Context? = null): String {
    val logCal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
    val todayCal = java.util.Calendar.getInstance()
    val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val dayBeforeCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -2) }

    val isSameDay = logCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
            logCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR)

    val isYesterday = logCal.get(java.util.Calendar.YEAR) == yesterdayCal.get(java.util.Calendar.YEAR) &&
            logCal.get(java.util.Calendar.DAY_OF_YEAR) == yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR)

    val isDayBefore = logCal.get(java.util.Calendar.YEAR) == dayBeforeCal.get(java.util.Calendar.YEAR) &&
            logCal.get(java.util.Calendar.DAY_OF_YEAR) == dayBeforeCal.get(java.util.Calendar.DAY_OF_YEAR)

    return when {
        isSameDay -> context?.getString(R.string.ledger_day_today) ?: "اليوم"
        isYesterday -> context?.getString(R.string.ledger_day_yesterday) ?: "الأمس"
        isDayBefore -> context?.getString(R.string.ledger_day_before_yesterday) ?: "أول أمس"
        else -> {
            val dayName = java.text.SimpleDateFormat("EEEE", java.util.Locale("ar")).format(java.util.Date(timestampMs))
            val dateNumbers = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ENGLISH).format(java.util.Date(timestampMs))
            "$dayName، $dateNumbers"
        }
    }
}

fun formatAuditLogTime(timestampMs: Long): String {
    val date = java.util.Date(timestampMs)
    val datePart = java.text.SimpleDateFormat("dd-MM-yyyy | hh:mm", java.util.Locale.ENGLISH).format(date)
    val amPmPart = java.text.SimpleDateFormat("a", java.util.Locale("ar")).format(date)
    return "$datePart $amPmPart"
}
