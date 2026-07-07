package com.example.domain.usecase

import com.example.data.repository.FinanceRepository
import com.example.data.local.entities.TransactionDb

class DeleteHabayebTransactionUseCase(private val repository: FinanceRepository) {
    suspend fun execute(txId: String, mainTransactions: List<TransactionDb>): Boolean {
        return try {
            repository.getHabayebTransactionById(txId)?.let { tx ->
                repository.softDeleteHabayebTransactionToTrash(tx)
                if (tx.linkedMainTxId != null) {
                    mainTransactions.find { it.id == tx.linkedMainTxId }?.let {
                        repository.softDeleteTransactionToTrash(it)
                    }
                    repository.deleteTransactionById(tx.linkedMainTxId)
                }
                repository.deleteHabayebTransactionById(txId)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
