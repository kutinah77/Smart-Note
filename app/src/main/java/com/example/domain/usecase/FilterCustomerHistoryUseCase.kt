package com.example.domain.usecase

import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import java.util.Calendar

class FilterCustomerHistoryUseCase {
    fun execute(
        allCustomerTxs: List<HabayebTransaction>,
        txSearchQuery: String,
        dateFilterMode: Int,
        customStartDate: Long?,
        customEndDate: Long?,
        typeFilterMode: Int,
        selectedCurrencyFilter: String?,
        currencySymbol: String,
        searchDebtStr: String,
        searchPaymentStr: String
    ): List<HabayebTransaction> {
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        val todayEnd = todayStart + 86400

        val calendarMonth = Calendar.getInstance()
        val monthStart = calendarMonth.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        calendarMonth.add(Calendar.MONTH, 1)
        val monthEnd = calendarMonth.timeInMillis / 1000

        val baseFiltered = allCustomerTxs.filter { tx ->
            // 1. Search Query filter
            val matchesSearch = if (txSearchQuery.isBlank()) {
                true
            } else {
                tx.description.contains(txSearchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(txSearchQuery) ||
                tx.foreign_amount.toString().contains(txSearchQuery) ||
                (if (tx.type == "OWED_BY_THEM" || tx.type == "OWED_TO_THEM") searchDebtStr else searchPaymentStr).contains(txSearchQuery, ignoreCase = true)
            }

            // 2. Date filter
            val matchesDate = when (dateFilterMode) {
                1 -> tx.timestamp in todayStart..todayEnd // Today
                2 -> tx.timestamp in monthStart..monthEnd // This Month
                3 -> { // Custom range
                    val startSec = (customStartDate ?: 0L) / 1000
                    val endSec = if (customEndDate != null) (customEndDate / 1000) + 86400 else Long.MAX_VALUE
                    tx.timestamp in startSec..endSec
                }
                else -> true // All
            }

            // 3. Type filter
            val matchesType = when (typeFilterMode) {
                1 -> tx.type == "OWED_BY_THEM" || tx.type == "OWED_TO_THEM" // Debts
                2 -> tx.type == "PAYMENT_BY_THEM" || tx.type == "PAYMENT_TO_THEM" // Payments
                else -> true // All
            }

            // 4. Currency filter
            val matchesCurrency = if (selectedCurrencyFilter != null) {
                val (txCurrency, _) = CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                txCurrency == selectedCurrencyFilter
            } else {
                true
            }

            matchesSearch && matchesDate && matchesType && matchesCurrency
        }
        return baseFiltered.sortedByDescending { it.timestamp }
    }
}
