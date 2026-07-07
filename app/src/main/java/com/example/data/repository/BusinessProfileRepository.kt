package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

data class BusinessProfile(
    val name: String,
    val description: String,
    val logoPath: String,
    val phones: List<String>
)

class BusinessProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("business_profile", Context.MODE_PRIVATE)
    private val altPrefs: SharedPreferences = context.getSharedPreferences("business_profile_prefs", Context.MODE_PRIVATE)

    fun getProfile(): BusinessProfile {
        val bizName = prefs.getString("biz_name", "").orEmpty()
            .ifBlank { altPrefs.getString("business_name", "").orEmpty() }
        
        val bizDesc = prefs.getString("biz_desc", "").orEmpty()
            .ifBlank { altPrefs.getString("business_slogan", "").orEmpty() }

        val logoPath = prefs.getString("biz_logo_path", "").orEmpty()
            .ifBlank { altPrefs.getString("logo_path", "").orEmpty() }

        val phones = mutableListOf<String>()
        val phonesJson = prefs.getString("biz_phones", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(phonesJson)
            for (i in 0 until jsonArray.length()) {
                phones.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (phones.isEmpty()) {
            val fallbackPhone = altPrefs.getString("business_phone", "").orEmpty()
            if (fallbackPhone.isNotBlank()) {
                phones.add(fallbackPhone)
            }
        }

        return BusinessProfile(
            name = bizName,
            description = bizDesc,
            logoPath = logoPath,
            phones = phones
        )
    }

    fun saveProfile(profile: BusinessProfile) {
        // 1. Save to primary SharedPreferences
        val jsonArray = JSONArray()
        profile.phones.filter { it.isNotBlank() }.forEach { jsonArray.put(it.trim()) }

        prefs.edit()
            .putString("biz_name", profile.name.trim())
            .putString("biz_desc", profile.description.trim())
            .putString("biz_logo_path", profile.logoPath)
            .putString("biz_phones", jsonArray.toString())
            .apply()

        // 2. Save to secondary SharedPreferences to guarantee PDF report correctness
        val primaryPhone = profile.phones.firstOrNull { it.isNotBlank() } ?: ""
        altPrefs.edit()
            .putString("business_name", profile.name.trim())
            .putString("business_slogan", profile.description.trim())
            .putString("logo_path", profile.logoPath)
            .putString("business_phone", primaryPhone)
            .apply()
    }
}
