package com.example.ui.screens.habayeb.utils

import org.json.JSONObject

object ExchangeRateHelper {
    
    fun getRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Double {
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (root.has(baseCurrencySymbol)) {
                val baseObj = root.getJSONObject(baseCurrencySymbol)
                if (baseObj.has(foreignCurrencySymbol)) {
                    return baseObj.getDouble(foreignCurrencySymbol)
                }
            }
            1.0 // fallback
        } catch (e: Exception) {
            1.0
        }
    }

    fun hasRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Boolean {
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (root.has(baseCurrencySymbol)) {
                val baseObj = root.getJSONObject(baseCurrencySymbol)
                if (baseObj.has(foreignCurrencySymbol)) {
                    return baseObj.getDouble(foreignCurrencySymbol) > 0.0
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun setRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String, rate: Double): String {
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            val baseObj = if (root.has(baseCurrencySymbol)) {
                root.getJSONObject(baseCurrencySymbol)
            } else {
                JSONObject()
            }
            baseObj.put(foreignCurrencySymbol, rate)
            root.put(baseCurrencySymbol, baseObj)
            root.toString()
        } catch (e: Exception) {
            jsonStr
        }
    }
}
