package com.example.data.serialization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportTransaction(
    val index: Int,
    val dayName: String,
    val dateStr: String,
    val description: String,
    val isForeign: Boolean,
    val owedAmountStr: String,
    val paymentAmountStr: String,
    val runningBalanceStr: String,
    val isOwed: Boolean
)

data class CustomerReportState(
    val customerName: String,
    val businessName: String,
    val businessDesc: String,
    val businessPhones: String,
    val docDateText: String,
    val docTimeText: String,
    val logoBitmap: Bitmap?,
    val logoWidth: Float,
    val logoHeight: Float,
    val totalDebtsStr: String,
    val totalPaymentsStr: String,
    val netRemainingStr: String,
    val netStatus: String,
    val calculatedNetDebt: Double,
    val baseNetStr: String,
    val foreignCurrencyTotals: List<Pair<String, String>>,
    val transactions: List<ReportTransaction>,
    val defaultCurrency: String
) {
    companion object {
        fun from(
            context: Context,
            customer: HabayebCustomer,
            transactions: List<HabayebTransaction>
        ): CustomerReportState {
            val sortedTxs = transactions.sortedBy { it.timestamp }
            
            // Load business profile details
            val prefs = context.getSharedPreferences("business_profile", Context.MODE_PRIVATE)
            val bizName = prefs.getString("biz_name", "") ?: ""
            val bizDesc = prefs.getString("biz_desc", "") ?: ""
            val bizLogoPath = prefs.getString("biz_logo_path", "") ?: ""
            val bizPhones = mutableListOf<String>()
            try {
                val phonesJson = prefs.getString("biz_phones", "[]") ?: "[]"
                val jsonArray = JSONArray(phonesJson)
                for (i in 0 until jsonArray.length()) {
                    bizPhones.add(jsonArray.getString(i))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val displayedName = if (bizName.isNotBlank()) bizName else context.getString(R.string.app_name)
            val displayedDesc = if (bizDesc.isNotBlank()) bizDesc else context.getString(R.string.pdf_default_desc)

            var logoWidth = 0f
            var logoHeight = 0f
            var logoBitmap: Bitmap? = null

            if (bizLogoPath.isNotEmpty()) {
                try {
                    val logoFile = File(bizLogoPath)
                    if (logoFile.exists()) {
                        var rawBitmap: Bitmap? = null
                        if (logoFile.length() > 1024 * 1024) {
                            val original = BitmapFactory.decodeFile(logoFile.absolutePath)
                            if (original != null) {
                                val stream = ByteArrayOutputStream()
                                original.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                val byteArray = stream.toByteArray()
                                rawBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            }
                        } else {
                            rawBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                        }
                        
                        if (rawBitmap != null) {
                            val maxW = 70f
                            val maxH = 55f
                            val originalWidth = rawBitmap.width.toFloat()
                            val originalHeight = rawBitmap.height.toFloat()
                            val scale = (maxW / originalWidth).coerceAtMost(maxH / originalHeight)
                            val finalW = (originalWidth * scale).coerceAtLeast(1f)
                            val finalH = (originalHeight * scale).coerceAtLeast(1f)
                            
                            logoBitmap = Bitmap.createScaledBitmap(rawBitmap, finalW.toInt(), finalH.toInt(), true)
                            logoWidth = finalW
                            logoHeight = finalH
                            
                            try {
                                rawBitmap.recycle()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val phonesToDraw = if (bizPhones.isNotEmpty()) bizPhones else listOf(context.getString(R.string.pdf_certified_identity))
            val phonesStr = if (bizPhones.isNotEmpty()) {
                context.getString(R.string.pdf_phone_prefix) + " " + phonesToDraw.joinToString("  |  ")
            } else {
                phonesToDraw.joinToString("  |  ")
            }

            val dayNameFormatted = SimpleDateFormat("EEEE", Locale("ar")).format(Date())
            val dateOnlyFormatted = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date())
            val timeFormatted = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())

            val docDateText = context.getString(R.string.pdf_doc_date, dayNameFormatted, dateOnlyFormatted)
            val docTimeText = context.getString(R.string.pdf_doc_time, timeFormatted)

            // Calculations
            var totalDebts = 0.0
            var totalPayments = 0.0
            var totalDebtsBase = 0.0
            var totalPaymentsBase = 0.0
            val uncalculatedForeignSums = mutableMapOf<String, Double>()

            for (tx in sortedTxs) {
                val amountVal = if (tx.is_foreign) {
                    if (tx.is_rate_calculated) tx.equivalent_amount else 0.0
                } else {
                    tx.amount
                }
                
                val baseAmountVal = if (tx.is_foreign) 0.0 else tx.amount

                if (tx.type == "OWED_BY_THEM" || tx.type == "PAYMENT_TO_THEM") {
                    totalDebts += amountVal
                    totalDebtsBase += baseAmountVal
                    if (tx.is_foreign && !tx.is_rate_calculated) {
                        uncalculatedForeignSums[tx.currency_code] = (uncalculatedForeignSums[tx.currency_code] ?: 0.0) + tx.foreign_amount
                    }
                } else if (tx.type == "PAYMENT_BY_THEM" || tx.type == "OWED_TO_THEM") {
                    totalPayments += amountVal
                    totalPaymentsBase += baseAmountVal
                    if (tx.is_foreign && !tx.is_rate_calculated) {
                        uncalculatedForeignSums[tx.currency_code] = (uncalculatedForeignSums[tx.currency_code] ?: 0.0) - tx.foreign_amount
                    }
                }
            }

            val calculatedNetDebt = totalDebts - totalPayments
            val defaultCurrency = context.getString(R.string.pdf_default_currency)
            val totalDebtsStr = String.format(Locale.ENGLISH, "%,.2f", totalDebts) + " " + defaultCurrency
            val totalPaymentsStr = String.format(Locale.ENGLISH, "%,.2f", totalPayments) + " " + defaultCurrency
            val netRemainingStr = String.format(Locale.ENGLISH, "%,.2f", Math.abs(calculatedNetDebt)) + " " + defaultCurrency

            val netStatus = if (calculatedNetDebt > 0) {
                context.getString(R.string.pdf_status_owed_by_them)
            } else if (calculatedNetDebt < 0) {
                context.getString(R.string.pdf_status_owed_to_them)
            } else {
                context.getString(R.string.pdf_status_balanced)
            }

            val calculatedBaseNet = totalDebtsBase - totalPaymentsBase
            val baseStatus = if (calculatedBaseNet > 0) {
                context.getString(R.string.pdf_status_owed_by_them)
            } else if (calculatedBaseNet < 0) {
                context.getString(R.string.pdf_status_owed_to_them)
            } else {
                context.getString(R.string.pdf_status_balanced)
            }
            val baseNetStr = String.format(Locale.ENGLISH, "%,.2f", Math.abs(calculatedBaseNet)) + " " + defaultCurrency + " " + baseStatus

            // Independent totals for other uncalculated currencies
            val foreignCurrencyTotals = mutableListOf<Pair<String, String>>()
            for ((currency, sum) in uncalculatedForeignSums) {
                if (Math.abs(sum) > 0.001) {
                    val status = if (sum > 0) {
                        context.getString(R.string.pdf_status_owed_by_them)
                    } else if (sum < 0) {
                        context.getString(R.string.pdf_status_owed_to_them)
                    } else {
                        context.getString(R.string.pdf_status_balanced)
                    }
                    val formattedSum = String.format(Locale.ENGLISH, "%,.2f", Math.abs(sum)) + " " + currency + " " + status
                    foreignCurrencyTotals.add(Pair(currency, formattedSum))
                }
            }

            // Map Report Transactions
            var runningBal = 0.0
            val reportTxs = sortedTxs.mapIndexed { index, tx ->
                val amountVal = if (tx.is_foreign) {
                    if (tx.is_rate_calculated) tx.equivalent_amount else 0.0
                } else {
                    tx.amount
                }

                val isOwed = tx.type == "OWED_BY_THEM" || tx.type == "PAYMENT_TO_THEM"
                if (isOwed) {
                    runningBal += amountVal
                } else {
                    runningBal -= amountVal
                }

                val dayName = try {
                    SimpleDateFormat("EEEE", Locale("ar")).format(Date(tx.timestamp * 1000))
                } catch (e: Exception) {
                    ""
                }
                val formattedDate = try {
                    SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(tx.timestamp * 1000))
                } catch (e: Exception) {
                    ""
                }

                val txTypeStr = when (tx.type) {
                    "OWED_BY_THEM" -> context.getString(R.string.habayeb_pdf_tx_owed_by)
                    "PAYMENT_BY_THEM" -> context.getString(R.string.habayeb_pdf_tx_payment_by)
                    "OWED_TO_THEM" -> context.getString(R.string.habayeb_pdf_tx_owed_to)
                    "PAYMENT_TO_THEM" -> context.getString(R.string.habayeb_pdf_tx_payment_to)
                    else -> context.getString(R.string.habayeb_pdf_tx_generic)
                }
                val txLabel = buildString {
                    append(txTypeStr)
                    if (tx.description.isNotEmpty()) {
                        append(" - ")
                        append(tx.description)
                    }
                    if (tx.is_foreign) {
                        append("\n[${tx.foreign_amount} ${tx.currency_code}")
                        if (tx.is_rate_calculated) {
                            append(context.getString(R.string.pdf_rate_suffix, tx.exchange_rate.toString()))
                        } else {
                            append(context.getString(R.string.pdf_uncalculated_suffix))
                        }
                    }
                }

                val formattedAmount = if (tx.is_foreign) {
                    if (tx.is_rate_calculated) {
                        String.format(Locale.ENGLISH, "%,.0f", tx.equivalent_amount)
                    } else "-"
                } else {
                    String.format(Locale.ENGLISH, "%,.2f", tx.amount)
                }

                ReportTransaction(
                    index = index + 1,
                    dayName = dayName,
                    dateStr = formattedDate,
                    description = txLabel,
                    isForeign = tx.is_foreign,
                    owedAmountStr = if (isOwed) formattedAmount else "-",
                    paymentAmountStr = if (!isOwed) formattedAmount else "-",
                    runningBalanceStr = String.format(Locale.ENGLISH, "%,.2f", runningBal),
                    isOwed = isOwed
                )
            }

            return CustomerReportState(
                customerName = customer.name,
                businessName = displayedName,
                businessDesc = displayedDesc,
                businessPhones = phonesStr,
                docDateText = docDateText,
                docTimeText = docTimeText,
                logoBitmap = logoBitmap,
                logoWidth = logoWidth,
                logoHeight = logoHeight,
                totalDebtsStr = totalDebtsStr,
                totalPaymentsStr = totalPaymentsStr,
                netRemainingStr = netRemainingStr,
                netStatus = netStatus,
                calculatedNetDebt = calculatedNetDebt,
                baseNetStr = baseNetStr,
                foreignCurrencyTotals = foreignCurrencyTotals,
                transactions = reportTxs,
                defaultCurrency = defaultCurrency
            )
        }
    }
}
