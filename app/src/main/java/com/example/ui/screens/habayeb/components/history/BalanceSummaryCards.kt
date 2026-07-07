package com.example.ui.screens.habayeb.components.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.local.entities.HabayebCustomer
import com.example.ui.screens.habayeb.components.CustomerSummaryCard

@Composable
fun BalanceSummary(
    activeCustomer: HabayebCustomer,
    currencySymbol: String,
    netDebtMap: Map<String, Double>,
    selectedCurrencyFilter: String?,
    onCurrencyFilterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    CustomerSummaryCard(
        activeCustomer = activeCustomer,
        currencySymbol = currencySymbol,
        netDebtMap = netDebtMap,
        selectedCurrencyFilter = selectedCurrencyFilter,
        onCurrencyFilterSelected = onCurrencyFilterSelected
    )
}
