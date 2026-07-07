package com.example.domain.usecase

import androidx.room.withTransaction
import com.example.data.repository.FinanceRepository
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import java.math.BigDecimal
import java.util.UUID

class ProcessHabayebTransactionUseCase(private val repository: FinanceRepository) {

    sealed class ProcessResult {
        object Success : ProcessResult()
        data class ValidationError(val messageResId: Int) : ProcessResult()
        data class Error(val message: String) : ProcessResult()
    }

    suspend fun execute(
        customerId: String,
        type: String,
        amountStr: String,
        descStr: String,
        dateMillis: Long,
        editingTransaction: HabayebTransaction?,
        selectedTransactionCurrency: String,
        currencySymbol: String,
        applyExchangeRate: Boolean,
        settingsRate: Double
    ): ProcessResult {
        return try {
            val cleanAmountStr = CurrencyConfig.normalizeDigits(amountStr).trim()
            val amountBD = try {
                if (cleanAmountStr.isBlank()) BigDecimal.ZERO else BigDecimal(cleanAmountStr)
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
            val amount = amountBD.toDouble()

            // 1. Validation
            if (amount <= 0.0 && descStr.trim().isBlank()) {
                return ProcessResult.ValidationError(com.example.R.string.habayeb_toast_amount_or_details_required)
            }
            if (amount < 0.0) {
                return ProcessResult.ValidationError(com.example.R.string.habayeb_toast_valid_amount)
            }

            // 2. Calculations using BigDecimal
            val isForeignSelected = selectedTransactionCurrency != currencySymbol
            val isRateCalculated = isForeignSelected && applyExchangeRate

            val finalEquivalentAmountBD = if (isRateCalculated) {
                amountBD.multiply(BigDecimal.valueOf(settingsRate))
            } else {
                BigDecimal.ZERO
            }
            val finalEquivalentAmount = finalEquivalentAmountBD.toDouble()

            val finalAmount = if (isRateCalculated) finalEquivalentAmount else amount
            val formattedDesc = CurrencyConfig.formatDescriptionWithCurrency(descStr.trim(), selectedTransactionCurrency)

            // 3. Atomically execute Save/Update
            repository.database.withTransaction {
                val transactionId = editingTransaction?.id ?: "dtx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
                val linkedMainTxId = editingTransaction?.linkedMainTxId

                // Update linked main transaction if exists
                if (linkedMainTxId != null) {
                    repository.getTransactionById(linkedMainTxId)?.let { mainTx ->
                        repository.saveTransaction(mainTx.copy(amount = finalAmount, timestamp = dateMillis / 1000))
                    }
                }

                val targetTransaction = HabayebTransaction(
                    id = transactionId,
                    customerId = customerId,
                    type = type,
                    amount = finalAmount,
                    timestamp = dateMillis / 1000,
                    description = formattedDesc,
                    linkedMainTxId = linkedMainTxId,
                    is_foreign = isForeignSelected,
                    currency_code = selectedTransactionCurrency,
                    foreign_amount = amount,
                    exchange_rate = if (applyExchangeRate) settingsRate else 1.0,
                    is_rate_calculated = isRateCalculated,
                    equivalent_amount = finalEquivalentAmount
                )

                repository.insertHabayebTransaction(targetTransaction)
            }

            ProcessResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            ProcessResult.Error(e.message ?: "Unknown error occurred")
        }
    }
}
