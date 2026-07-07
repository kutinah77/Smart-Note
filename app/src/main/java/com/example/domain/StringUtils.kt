package com.example.domain

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Shared utility functions for text normalization, particularly for Arabic language character mapping.
 */
object StringUtils {
    @JvmStatic
    fun normalizeArabic(text: String, context: android.content.Context? = null): String {
        val alefHamzaAbove = context?.getString(com.example.R.string.char_alef_hamza_above) ?: "أ"
        val alefHamzaBelow = context?.getString(com.example.R.string.char_alef_hamza_below) ?: "إ"
        val alefMadda = context?.getString(com.example.R.string.char_alef_madda) ?: "آ"
        val taaMarboota = context?.getString(com.example.R.string.char_taa_marboota) ?: "ة"
        val alefMaksoura = context?.getString(com.example.R.string.char_alef_maksoura) ?: "ى"
        val alef = context?.getString(com.example.R.string.char_alef) ?: "ا"
        val haa = context?.getString(com.example.R.string.char_haa) ?: "ه"
        val yaa = context?.getString(com.example.R.string.char_yaa) ?: "ي"

        return text.replace(alefHamzaAbove, alef)
            .replace(alefHamzaBelow, alef)
            .replace(alefMadda, alef)
            .replace(taaMarboota, haa)
            .replace(alefMaksoura, yaa)
            .trim()
    }

    @JvmStatic
    fun getContactDetails(context: android.content.Context, contactUri: android.net.Uri): Pair<String, String>? {
        var name = ""
        var phone = ""
        try {
            val cr = context.contentResolver
            cr.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                    
                    val idIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                    if (idIndex >= 0) {
                        val contactId = cursor.getString(idIndex)
                        val hasPhoneIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val hasPhone = if (hasPhoneIndex >= 0) cursor.getString(hasPhoneIndex) else null
                        
                        if (hasPhone == "1") {
                            cr.query(
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val numberIndex = phoneCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numberIndex >= 0) {
                                        phone = phoneCursor.getString(numberIndex) ?: ""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StringUtils", "Error fetching contact: ${e.message}", e)
        }

        try {
            val cleanedPhone = phone.replace(Regex("[^0-9+]"), "")
            if (name.isNotEmpty()) {
                return Pair(name, cleanedPhone)
            }
        } catch (e: Exception) {
            android.util.Log.e("StringUtils", "Error sanitizing contact phone: ${e.message}", e)
        }

        if (name.isNotEmpty()) {
            return Pair(name, "")
        }
        return null
    }

    @JvmStatic
    fun String.toEnglishDigits(): String {
        var result = this
        val arabicIndicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val westernDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        for (i in 0..9) {
            result = result.replace(arabicIndicDigits[i], westernDigits[i])
        }
        return result
    }
}

/**
 * Shared formatting utilities for displaying monetary values/currency.
 */
object FormatUtils {
    @JvmStatic
    fun formatCurrency(amount: BigDecimal, symbol: String = "", context: android.content.Context? = null): String {
        val finalSymbol = symbol.ifEmpty { context?.getString(com.example.R.string.currency_yer) ?: "ر.ي" }
        return try {
            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val formatter = DecimalFormat("#,##0", symbols)
            val formatted = formatter.format(amount)
            "$formatted $finalSymbol"
        } catch (e: Exception) {
            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val formatter = DecimalFormat("#,##0", symbols)
            val formatted = formatter.format(amount)
            "$formatted $finalSymbol"
        }
    }

    @JvmStatic
    fun formatDoubleCurrency(amount: Double, symbol: String = "", context: android.content.Context? = null): String {
        val finalSymbol = symbol.ifEmpty { context?.getString(com.example.R.string.currency_yer) ?: "ر.ي" }
        return try {
            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val formatter = DecimalFormat("#,##0", symbols)
            val formatted = formatter.format(amount)
            "$formatted $finalSymbol"
        } catch (e: Exception) {
            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val formatter = DecimalFormat("#,##0", symbols)
            val formatted = formatter.format(amount)
            "$formatted $finalSymbol"
        }
    }
}
