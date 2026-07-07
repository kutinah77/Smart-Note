package com.example.domain.usecase

import com.example.data.repository.FinanceRepository
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import java.util.UUID

class SaveHabayebCustomerUseCase(private val repository: FinanceRepository) {
    suspend fun execute(
        customer: HabayebCustomer,
        initialAmount: Double,
        initialType: String,
        customTimestamp: Long,
        initialDetails: String,
        isForeign: Boolean,
        currencyCode: String,
        foreignAmount: Double,
        exchangeRate: Double,
        isRateCalculated: Boolean,
        equivalentAmount: Double
    ): Boolean {
        return try {
            val transaction = if (initialAmount > 0.0) {
                HabayebTransaction(
                    id = "dtx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                    customerId = customer.id,
                    type = initialType,
                    amount = initialAmount,
                    timestamp = customTimestamp,
                    description = initialDetails.ifEmpty { customer.notes },
                    is_foreign = isForeign,
                    currency_code = currencyCode,
                    foreign_amount = foreignAmount,
                    exchange_rate = exchangeRate,
                    is_rate_calculated = isRateCalculated,
                    equivalent_amount = equivalentAmount
                )
            } else null
            repository.insertCustomerWithOpeningTransaction(customer, transaction)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
