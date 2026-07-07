package com.example.domain.usecase

import com.example.data.repository.FinanceRepository
import com.example.data.local.entities.HabayebTransaction
import java.util.UUID

class AddHabayebTransactionUseCase(private val repository: FinanceRepository) {
    suspend fun execute(
        customerId: String,
        type: String,
        amount: Double,
        desc: String,
        timestamp: Long,
        linkedMainTxId: String?,
        isForeign: Boolean,
        currencyCode: String,
        foreignAmount: Double,
        exchangeRate: Double,
        isRateCalculated: Boolean,
        equivalentAmount: Double
    ): Boolean {
        return try {
            repository.insertHabayebTransaction(
                HabayebTransaction(
                    id = "dtx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                    customerId = customerId,
                    type = type,
                    amount = amount,
                    timestamp = timestamp,
                    description = desc,
                    linkedMainTxId = linkedMainTxId,
                    is_foreign = isForeign,
                    currency_code = currencyCode,
                    foreign_amount = foreignAmount,
                    exchange_rate = exchangeRate,
                    is_rate_calculated = isRateCalculated,
                    equivalent_amount = equivalentAmount
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
