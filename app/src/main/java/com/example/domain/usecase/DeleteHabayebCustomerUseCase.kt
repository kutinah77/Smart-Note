package com.example.domain.usecase

import com.example.data.repository.FinanceRepository
import com.example.data.local.entities.HabayebCustomer

class DeleteHabayebCustomerUseCase(private val repository: FinanceRepository) {
    suspend fun executeSingle(customerId: String, customers: List<HabayebCustomer>): Boolean {
        return try {
            val customer = customers.find { it.id == customerId }
            val customerTxs = repository.getAllTransactionsDirect().filter { it.customerId == customerId }
            if (customer != null) {
                repository.softDeleteHabayebBundleToTrash(customer, customerTxs)
            }
            repository.deleteCustomerAndTransactions(customerId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun executeMultiple(customerIds: List<String>, customers: List<HabayebCustomer>): Boolean {
        return try {
            val allTxs = repository.getAllTransactionsDirect()
            for (id in customerIds) {
                val customer = customers.find { it.id == id }
                if (customer != null) {
                    repository.softDeleteHabayebBundleToTrash(customer, allTxs.filter { it.customerId == id })
                }
                repository.deleteCustomerAndTransactions(id)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
