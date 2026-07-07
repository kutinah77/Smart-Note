package com.example.domain.usecase

import com.example.data.local.entities.HabayebCustomer
import java.util.UUID

class CreateHabayebCustomerUseCase(
    private val saveHabayebCustomerUseCase: SaveHabayebCustomerUseCase
) {
    suspend fun execute(
        name: String,
        phone: String,
        notes: String,
        initialAmount: Double,
        initialType: String,
        selectedCurrency: String,
        currencySymbol: String,
        applyExchangeRate: Boolean,
        settingsRate: Double,
        customTimestamp: Long,
        defaultOpeningDetails: String,
        formatDescriptionWithCurrency: (String, String) -> String
    ): Pair<Boolean, String?> {
        if (name.trim().isBlank()) {
            return Pair(false, null)
        }

        val customerId = "cust_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        
        val isForeignSelected = selectedCurrency != currencySymbol
        val exchangeRate = if (isForeignSelected && applyExchangeRate) {
            settingsRate
        } else {
            1.0
        }
        val finalEquivalentAmount = if (isForeignSelected && applyExchangeRate) {
            initialAmount * exchangeRate
        } else {
            0.0
        }
        val finalAmount = if (isForeignSelected && applyExchangeRate) finalEquivalentAmount else initialAmount

        val cleanNotes = notes.trim()
        val finalDetails = cleanNotes.ifEmpty { defaultOpeningDetails }
        val formattedDetails = formatDescriptionWithCurrency(finalDetails, selectedCurrency)

        val newCustomer = HabayebCustomer(
            id = customerId,
            name = name.trim(),
            phone = phone.trim(),
            notes = cleanNotes,
            createdAt = customTimestamp,
            initialType = initialType
        )

        val success = saveHabayebCustomerUseCase.execute(
            customer = newCustomer,
            initialAmount = finalAmount,
            initialType = initialType,
            customTimestamp = customTimestamp,
            initialDetails = formattedDetails,
            isForeign = isForeignSelected,
            currencyCode = selectedCurrency,
            foreignAmount = initialAmount,
            exchangeRate = exchangeRate,
            isRateCalculated = isForeignSelected && applyExchangeRate,
            equivalentAmount = finalEquivalentAmount
        )

        return Pair(success, if (success) customerId else null)
    }
}
