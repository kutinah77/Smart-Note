package com.example.domain.service

import android.content.Context
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.habayeb.utils.ExchangeRateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CurrencyMigrationService(private val repository: FinanceRepository) {

    suspend fun migrateBaseCurrency(
        context: Context,
        oldBase: String,
        newBase: String,
        exchangeRatesJson: String
    ): String = withContext(Dispatchers.IO) {
        if (oldBase == newBase) return@withContext exchangeRatesJson

        // 1. Convert rates relative to newBase
        val rateNewBaseOldBase = ExchangeRateHelper.getRate(exchangeRatesJson, oldBase, newBase)
        
        val newRatesJson = try {
            val root = JSONObject(if (exchangeRatesJson.isBlank()) "{}" else exchangeRatesJson)
            val newBaseObj = JSONObject()
            
            if (root.has(oldBase)) {
                val oldBaseObj = root.getJSONObject(oldBase)
                val keys = oldBaseObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key != newBase) {
                        val oldRate = oldBaseObj.getDouble(key)
                        val newRate = if (rateNewBaseOldBase > 0.0) oldRate / rateNewBaseOldBase else oldRate
                        newBaseObj.put(key, newRate)
                    }
                }
            }
            // Add oldBase itself back! 1 unit of oldBase = (1 / rateNewBaseOldBase) units of newBase.
            if (rateNewBaseOldBase > 0.0) {
                newBaseObj.put(oldBase, 1.0 / rateNewBaseOldBase)
            }
            
            root.put(newBase, newBaseObj)
            root.toString()
        } catch (e: Exception) {
            exchangeRatesJson
        }

        // 2. We should also update equivalent_amount of all transactions in database!
        try {
            val allTxs = repository.getAllTransactionsDirect()
            for (tx in allTxs) {
                if (tx.is_foreign) {
                    if (tx.is_rate_calculated) {
                        val oldEquiv = tx.equivalent_amount
                        val newEquiv = if (rateNewBaseOldBase > 0.0) oldEquiv / rateNewBaseOldBase else oldEquiv
                        val oldTxRate = tx.exchange_rate
                        val newTxRate = if (rateNewBaseOldBase > 0.0) oldTxRate / rateNewBaseOldBase else oldTxRate
                        
                        val updatedTx = tx.copy(
                            exchange_rate = newTxRate,
                            equivalent_amount = newEquiv,
                            amount = newEquiv // Since is_rate_calculated is true, amount equals equivalent_amount
                        )
                        repository.insertHabayebTransaction(updatedTx)
                        
                        // Also update linked main transaction if exists
                        if (tx.linkedMainTxId != null) {
                            repository.getTransactionById(tx.linkedMainTxId)?.let { mainTx ->
                                repository.saveTransaction(mainTx.copy(amount = newEquiv))
                            }
                        }
                    }
                } else {
                    // Convert old base local transactions to foreign in oldBase
                    val oldTxCode = tx.currency_code
                    if (!tx.is_foreign && (oldTxCode.isBlank() || oldTxCode == "DEFAULT" || oldTxCode == oldBase)) {
                        val updatedTx = tx.copy(
                            is_foreign = true,
                            currency_code = oldBase,
                            foreign_amount = tx.amount,
                            is_rate_calculated = false,
                            equivalent_amount = 0.0
                        )
                        repository.insertHabayebTransaction(updatedTx)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext newRatesJson
    }

    fun getSettingsRate(
        selectedCurrency: String,
        settings: com.example.data.local.entities.AppSettings,
        sarLabel: String,
        usdLabel: String,
        yerLabel: String
    ): Double {
        return when (selectedCurrency) {
            sarLabel -> if (settings.exchangeRateSar <= 0.0) 160.0 else settings.exchangeRateSar
            usdLabel -> if (settings.exchangeRateUsd <= 0.0) 600.0 else settings.exchangeRateUsd
            yerLabel -> if (settings.exchangeRateYer <= 0.0) 1.0 else settings.exchangeRateYer
            else -> 1.0
        }
    }

    fun hasRate(exchangeRatesJson: String, baseCurrency: String, targetCurrency: String): Boolean {
        return ExchangeRateHelper.hasRate(exchangeRatesJson, baseCurrency, targetCurrency)
    }

    fun getRate(exchangeRatesJson: String, baseCurrency: String, targetCurrency: String): Double {
        return ExchangeRateHelper.getRate(exchangeRatesJson, baseCurrency, targetCurrency)
    }

    fun setRate(exchangeRatesJson: String, baseCurrency: String, targetCurrency: String, rate: Double): String {
        return ExchangeRateHelper.setRate(exchangeRatesJson, baseCurrency, targetCurrency, rate)
    }
}
