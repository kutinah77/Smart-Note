package com.example.domain.usecase

import com.example.data.local.entities.TransactionDb
import com.example.domain.DateUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

data class MonthLedger(
    val monthKey: String, // "yyyy-MM"
    val monthName: String, // e.g. "يونيو 2026"
    val forwardedBalance: BigDecimal, // Forwarded sum from previous month
    val netAmount: BigDecimal, // Net for this month
    val finalBalance: BigDecimal, // netAmount + forwardedBalance
    val days: List<DayLedger>
)

data class DayLedger(
    val dayNumber: Int,
    val dayOfWeek: String, // "السبت" etc
    val fullDate: String, // "2026-06-01"
    val netAmount: BigDecimal,
    val transactions: List<TransactionDb>
)

class CalculateLedgerUseCase {

    fun execute(txList: List<TransactionDb>): List<MonthLedger> {
        // Sort chronic ascending to compute running balances correctly, then format descending for display
        val chronicTx = txList.sortedBy { it.timestamp }
        
        // Map of "yyyy-MM" -> List of Transactions
        val groupedByMonth = chronicTx.groupBy { DateUtils.getYearMonthKey(it.timestamp) }
        
        // Sorted months chronic ascending
        val sortedMonthKeys = groupedByMonth.keys.sorted()
        
        var runningForwardedBalance = BigDecimal.ZERO
        val ledgerList = mutableListOf<MonthLedger>()
        
        for (monthKey in sortedMonthKeys) {
            val monthTx = groupedByMonth[monthKey] ?: emptyList()
            val monthName = DateUtils.getMonthNameArabic(monthTx.first().timestamp)
            
            // Group transactions inside this month by Day of Month
            val groupedByDay = monthTx.groupBy { DateUtils.getDayOfMonth(it.timestamp) }
            
            // Days sorted descending (latest day first inside a month)
            val sortedDays = groupedByDay.keys.sortedDescending()
            
            val dayItems = mutableListOf<DayLedger>()
            var monthIncomes = BigDecimal.ZERO
            var monthExpenses = BigDecimal.ZERO
            
            for (day in sortedDays) {
                val dayTx = groupedByDay[day] ?: emptyList()
                val dayTimestamp = dayTx.first().timestamp
                val dayDateText = DateUtils.formatDateFull(dayTimestamp)
                val dayOfWeek = DateUtils.getDayOfWeekArabic(dayTimestamp)
                
                // Calc net for this day
                var dayIncome = BigDecimal.ZERO
                var dayExpense = BigDecimal.ZERO
                for (tx in dayTx) {
                    if (tx.type == "INCOME") {
                        dayIncome = dayIncome.add(BigDecimal.valueOf(tx.amount))
                    } else {
                        dayExpense = dayExpense.add(BigDecimal.valueOf(tx.amount))
                    }
                }
                val netDay = dayIncome.subtract(dayExpense)
                
                dayItems.add(
                    DayLedger(
                        dayNumber = day,
                        dayOfWeek = dayOfWeek,
                        fullDate = dayDateText,
                        netAmount = netDay,
                        transactions = dayTx.sortedByDescending { it.timestamp }
                    )
                )
                
                monthIncomes = monthIncomes.add(dayIncome)
                monthExpenses = monthExpenses.add(dayExpense)
            }
            
            val currentMonthNet = monthIncomes.subtract(monthExpenses)
            val totalForwarded = runningForwardedBalance
            val monthFinalBalance = totalForwarded.add(currentMonthNet)
            
            ledgerList.add(
                MonthLedger(
                    monthKey = monthKey,
                    monthName = monthName,
                    forwardedBalance = totalForwarded,
                    netAmount = currentMonthNet,
                    finalBalance = monthFinalBalance,
                    days = dayItems
                )
            )
            
            // Set forwarded balance for the next month to be the final balance of this month
            runningForwardedBalance = monthFinalBalance
        }
        
        // Return sorted descending by month so the newest month shows first
        return ledgerList.sortedByDescending { it.monthKey }
    }

    fun calculateSumByType(transactions: List<TransactionDb>, type: String): BigDecimal {
        var sum = BigDecimal.ZERO
        for (tx in transactions) {
            if (tx.type == type) {
                sum = sum.add(BigDecimal.valueOf(tx.amount))
            }
        }
        return sum.setScale(2, RoundingMode.HALF_EVEN)
    }

    fun calculateTotalCash(transactions: List<TransactionDb>): BigDecimal {
        val income = calculateSumByType(transactions, "INCOME")
        val expense = calculateSumByType(transactions, "EXPENSE")
        return income.subtract(expense).setScale(2, RoundingMode.HALF_EVEN)
    }

    fun calculateDailyExpenseComparison(transactions: List<TransactionDb>): Pair<BigDecimal, BigDecimal> {
        val todayKey = DateUtils.formatDateFull(System.currentTimeMillis() / 1000)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = DateUtils.formatDateFull(cal.timeInMillis / 1000)

        var todayExpenses = BigDecimal.ZERO
        var yesterdayExpenses = BigDecimal.ZERO

        for (tx in transactions) {
            if (tx.type == "EXPENSE") {
                val txDate = DateUtils.formatDateFull(tx.timestamp)
                if (txDate == todayKey) {
                    todayExpenses = todayExpenses.add(BigDecimal.valueOf(tx.amount))
                } else if (txDate == yesterdayKey) {
                    yesterdayExpenses = yesterdayExpenses.add(BigDecimal.valueOf(tx.amount))
                }
            }
        }
        return Pair(todayExpenses, yesterdayExpenses)
    }
}
