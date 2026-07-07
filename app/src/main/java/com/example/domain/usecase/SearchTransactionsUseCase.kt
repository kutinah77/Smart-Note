package com.example.domain.usecase

import android.content.Context
import com.example.data.local.entities.TransactionDb
import com.example.domain.StringUtils

class SearchTransactionsUseCase {
    fun execute(transactions: List<TransactionDb>, query: String, context: Context): List<TransactionDb> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = StringUtils.normalizeArabic(query, context)
        return transactions.filter { tx ->
            StringUtils.normalizeArabic(tx.description, context).contains(normalizedQuery, ignoreCase = true)
        }.sortedByDescending { it.timestamp }
    }
}
