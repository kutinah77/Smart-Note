package com.example.domain.usecase

import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.CurrencyConfig

class CalculateRunningBalanceUseCase {
    fun execute(allCustomerTxs: List<HabayebTransaction>, currencySymbol: String): Map<String, Double> {
        val chronological = allCustomerTxs.sortedBy { it.timestamp }
        val balancesMap = mutableMapOf<String, Double>()
        val currentBalMap = mutableMapOf<String, Double>()
        for (tx in chronological) {
            val (txCurrency, amountVal) = CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
            var currentBal = currentBalMap[txCurrency] ?: 0.0
            when (tx.type) {
                "OWED_BY_THEM" -> currentBal += amountVal
                "PAYMENT_BY_THEM" -> currentBal -= amountVal
                "OWED_TO_THEM" -> currentBal -= amountVal
                "PAYMENT_TO_THEM" -> currentBal += amountVal
            }
            currentBalMap[txCurrency] = currentBal
            balancesMap[tx.id] = currentBal
        }
        return balancesMap
    }
}
