package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.NavigationPreferences
import com.example.ui.viewmodel.FinanceConstants
import kotlinx.coroutines.flow.Flow

class AppPreferencesRepository(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(FinanceConstants.PREFS_NAME, Context.MODE_PRIVATE)
    private val secPrefs: SharedPreferences = context.getSharedPreferences("mizan_sec_prefs", Context.MODE_PRIVATE)
    private val navigationPrefs = NavigationPreferences(context)

    val tabOrderFlow: Flow<String> = navigationPrefs.tabOrderFlow
    val defaultStartFlow: Flow<String> = navigationPrefs.defaultStartFlow

    suspend fun saveTabOrder(order: String) {
        navigationPrefs.saveTabOrder(order)
    }

    suspend fun saveDefaultStart(start: String) {
        navigationPrefs.saveDefaultStart(start)
    }

    fun isCategoriesPopulated(): Boolean {
        return sharedPrefs.getBoolean("categories_populated", false)
    }

    fun setCategoriesPopulated(populated: Boolean) {
        sharedPrefs.edit().putBoolean("categories_populated", populated).apply()
    }

    fun isLinkHabayebDebtsEnabled(): Boolean {
        return sharedPrefs.getBoolean(FinanceConstants.KEY_LINK_HABAYEB_DEBTS, false)
    }

    fun setLinkHabayebDebtsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(FinanceConstants.KEY_LINK_HABAYEB_DEBTS, enabled).apply()
    }

    fun hasShownOnboarding(): Boolean {
        return sharedPrefs.getBoolean(FinanceConstants.KEY_ONBOARDING_SHOWN, false)
    }

    fun setOnboardingShown(shown: Boolean) {
        sharedPrefs.edit().putBoolean(FinanceConstants.KEY_ONBOARDING_SHOWN, shown).apply()
    }

    fun getActivationCode(): String {
        return secPrefs.getString("m_act_code", "") ?: ""
    }

    fun saveActivationCode(code: String, isPermanent: Boolean) {
        secPrefs.edit()
            .putBoolean("is_premium", true)
            .putBoolean("is_permanent", isPermanent)
            .putString("m_act_code", code)
            .apply()
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        secPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        secPrefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
