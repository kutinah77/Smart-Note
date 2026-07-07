package com.example.domain.usecase

import com.example.ui.state.CustomerUiState

class FilterHabayebCustomersUseCase {

    fun execute(
        customers: List<CustomerUiState>,
        searchQuery: String,
        selectedFilterTab: Int,
        financialSortMode: Int,
        historicalSortMode: Int,
        temporarilyHiddenCustomerIds: List<String> = emptyList()
    ): List<CustomerUiState> {
        val filteredList = customers.filter { customerUi ->
            if (temporarilyHiddenCustomerIds.contains(customerUi.id)) return@filter false
            val matchesSearch = searchQuery.isEmpty() ||
                    customerUi.name.contains(searchQuery, ignoreCase = true) ||
                    customerUi.phone.contains(searchQuery, ignoreCase = true)
            if (!matchesSearch) return@filter false

            when (selectedFilterTab) {
                1 -> customerUi.netDebt > 0.0 // Debtors (لي عند الناس)
                2 -> customerUi.netDebt < 0.0 // Creditors (علي للناس)
                else -> true
            }
        }

        return if (financialSortMode != 0) {
            if (financialSortMode == 1) {
                filteredList.sortedByDescending { kotlin.math.abs(it.netDebt) }
            } else {
                filteredList.sortedBy { kotlin.math.abs(it.netDebt) }
            }
        } else if (historicalSortMode != 0) {
            if (historicalSortMode == 1) {
                filteredList.sortedByDescending { it.lastTransactionTimestamp }
            } else {
                filteredList.sortedBy { it.lastTransactionTimestamp }
            }
        } else {
            filteredList
        }
    }
}
