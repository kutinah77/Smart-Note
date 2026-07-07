package com.example.domain.usecase

import com.example.data.local.entities.FixedCommitment
import java.math.BigDecimal

class CalculateCommitmentCoverageUseCase {
    fun execute(
        commitments: List<FixedCommitment>,
        totalCash: BigDecimal,
        linkHabayebDebts: Boolean,
        habayebOwedByThemTotal: Double
    ): List<Triple<FixedCommitment, Double, Double>> {
        var remainingCash = totalCash
        if (linkHabayebDebts) {
            remainingCash = remainingCash.add(BigDecimal.valueOf(habayebOwedByThemTotal))
        }

        return commitments.map { fc ->
            val target = BigDecimal.valueOf(fc.targetAmount)
            val alreadyPaid = BigDecimal.valueOf(fc.currentProgress)
            val needed = target.subtract(alreadyPaid).max(BigDecimal.ZERO)

            val allocatedFromCash = if (remainingCash >= scarcityCheck(needed)) {
                remainingCash = remainingCash.subtract(needed)
                needed
            } else if (remainingCash > BigDecimal.ZERO) {
                val temp = remainingCash
                remainingCash = BigDecimal.ZERO
                temp
            } else {
                BigDecimal.ZERO
            }
            val remaining = needed.subtract(allocatedFromCash)
            val totalCovered = alreadyPaid.add(allocatedFromCash)
            Triple(fc, totalCovered.toDouble(), remaining.toDouble())
        }
    }

    private fun scarcityCheck(needed: BigDecimal): BigDecimal {
        return needed
    }
}
