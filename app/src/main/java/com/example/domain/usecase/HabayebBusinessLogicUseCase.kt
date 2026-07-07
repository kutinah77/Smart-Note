package com.example.domain.usecase

import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.viewmodel.HabayebTransactionType
import com.example.ui.state.CustomerUiState
import com.example.ui.state.CustomersUiState
import java.math.BigDecimal

class HabayebBusinessLogicUseCase {

    fun execute(
        customers: List<HabayebCustomer>,
        transactions: List<HabayebTransaction>,
        settings: AppSettings
    ): CustomersUiState {
        val defaultCurrency = settings.currencySymbol
        val txsByCustomer = transactions.groupBy { it.customerId }
        val customerStates = customers.map { customer ->
            val custTxs = txsByCustomer[customer.id] ?: emptyList()
            val netDebtsByCurrency = mutableMapOf<String, BigDecimal>()
            for (tx in custTxs) {
                val (txCurrency, amountVal) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, defaultCurrency)
                val amount = BigDecimal.valueOf(amountVal)
                val currentVal = netDebtsByCurrency.getOrDefault(txCurrency, BigDecimal.ZERO)
                val updatedVal = when (tx.type) {
                    HabayebTransactionType.OWED_BY_THEM.name -> currentVal.add(amount)
                    HabayebTransactionType.PAYMENT_BY_THEM.name -> currentVal.subtract(amount)
                    HabayebTransactionType.OWED_TO_THEM.name -> currentVal.subtract(amount)
                    HabayebTransactionType.PAYMENT_TO_THEM.name -> currentVal.add(amount)
                    else -> currentVal
                }
                netDebtsByCurrency[txCurrency] = updatedVal
            }

            val netDebt = (netDebtsByCurrency[defaultCurrency] ?: BigDecimal.ZERO).toDouble()

            val nonZeroDebts = netDebtsByCurrency.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
            val (displayCurrency, displayDebtVal) = if (nonZeroDebts.isNotEmpty()) {
                if (nonZeroDebts.containsKey(defaultCurrency)) {
                    Pair(defaultCurrency, nonZeroDebts[defaultCurrency]!!.toDouble())
                } else {
                    val firstKey = nonZeroDebts.keys.first()
                    Pair(firstKey, nonZeroDebts[firstKey]!!.toDouble())
                }
            } else if (netDebtsByCurrency.isNotEmpty()) {
                if (netDebtsByCurrency.containsKey(defaultCurrency)) {
                    Pair(defaultCurrency, netDebtsByCurrency[defaultCurrency]!!.toDouble())
                } else {
                    val firstKey = netDebtsByCurrency.keys.first()
                    Pair(firstKey, netDebtsByCurrency[firstKey]!!.toDouble())
                }
            } else {
                Pair(defaultCurrency, 0.0)
            }

            val lastTxTime = custTxs.maxOfOrNull { it.timestamp } ?: customer.createdAt
            CustomerUiState(
                id = customer.id,
                name = customer.name,
                phone = customer.phone,
                notes = customer.notes,
                createdAt = customer.createdAt,
                totalTransactions = custTxs.size,
                netDebt = netDebt,
                displayNetDebt = displayDebtVal,
                displayCurrencySymbol = displayCurrency,
                lastTransactionTimestamp = lastTxTime,
                originalCustomer = customer
            )
        }
        var totalOwedByThem = BigDecimal.ZERO
        var totalOwedToThem = BigDecimal.ZERO
        customerStates.forEach { state ->
            val bdVal = BigDecimal.valueOf(state.netDebt)
            if (state.netDebt > 0.0) {
                totalOwedByThem = totalOwedByThem.add(bdVal)
            } else if (state.netDebt < 0.0) {
                totalOwedToThem = totalOwedToThem.add(bdVal.abs())
            }
        }

        return CustomersUiState(
            customers = customerStates,
            totalOwedByThem = totalOwedByThem,
            totalOwedToThem = totalOwedToThem,
            isLoading = false
        )
    }
}
