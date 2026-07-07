package com.example.data.serialization

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvReportGenerator {

    fun generateAndShareCsvReport(
        context: Context,
        scope: CoroutineScope,
        customer: HabayebCustomer,
        transactions: List<HabayebTransaction>,
        currencySymbol: String,
        onFinished: () -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            val file = generateCsvFileInternal(context, customer, transactions, currencySymbol)
            withContext(Dispatchers.Main) {
                if (file != null) {
                    triggerShareIntent(context, file, customer.name)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.habayeb_export_csv_failed, context.getString(R.string.csv_error_creating_file)),
                        Toast.LENGTH_LONG
                    ).show()
                }
                onFinished()
            }
        }
    }

    private fun generateCsvFileInternal(
        context: Context,
        customer: HabayebCustomer,
        transactions: List<HabayebTransaction>,
        currencySymbol: String
    ): File? {
        val fileName = "statement_${customer.name}_${System.currentTimeMillis() % 100000}.csv"
        val file = File(context.cacheDir, fileName)
        
        try {
            val fos = FileOutputStream(file)
            // Write UTF-8 BOM so Excel opens Arabic correctly
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            
            val writer = fos.bufferedWriter(Charsets.UTF_8)
            
            // CSV Title Header
            writer.write("\"${context.getString(R.string.csv_report_for_account, customer.name)}\"\n")
            val notRegStr = context.getString(R.string.csv_not_registered)
            writer.write("\"${context.getString(R.string.csv_phone_label, customer.phone.ifEmpty { notRegStr })}\"\n")
            val dateFormatted = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())
            writer.write("\"${context.getString(R.string.csv_report_date, dateFormatted)}\"\n\n")
            
            // Table Columns
            writer.write(context.getString(R.string.csv_header_cols, currencySymbol))
            
            // Sort transactions in chronological order to calculate correct running balance
            val chronological = transactions.sortedBy { it.timestamp }
            var runningBalance = java.math.BigDecimal.ZERO
            
            chronological.forEachIndexed { index, tx ->
                val (txCurrency, amountVal) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                val amountDecimal = com.example.ui.helper.HabayebMathHelper.toBigDecimal(amountVal)
                
                val typeName = when (tx.type) {
                    "OWED_BY_THEM" -> {
                        runningBalance = runningBalance.add(amountDecimal)
                        context.getString(R.string.csv_tx_owed_by_them)
                    }
                    "PAYMENT_BY_THEM" -> {
                        runningBalance = runningBalance.subtract(amountDecimal)
                        context.getString(R.string.csv_tx_payment_by_them)
                    }
                    "OWED_TO_THEM" -> {
                        runningBalance = runningBalance.subtract(amountDecimal)
                        context.getString(R.string.csv_tx_owed_to_them)
                    }
                    "PAYMENT_TO_THEM" -> {
                        runningBalance = runningBalance.add(amountDecimal)
                        context.getString(R.string.csv_tx_payment_to_them)
                    }
                    else -> context.getString(R.string.csv_tx_generic)
                }
                
                val dateStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date(tx.timestamp * 1000))
                val descriptionSanitized = tx.description.replace("\"", "\"\"").replace("\n", " ")
                
                val displayAmount = if (tx.is_foreign) tx.foreign_amount else tx.amount
                val currencyName = if (tx.is_foreign) tx.currency_code else currencySymbol
                val exchangeRateStr = if (tx.is_foreign) tx.exchange_rate.toString() else "1.0"
                val equivAmountStr = if (tx.is_foreign) tx.equivalent_amount.toString() else tx.amount.toString()
                
                writer.write("${index + 1},\"$dateStr\",\"$descriptionSanitized\",\"$typeName\",$displayAmount,\"$currencyName\",$exchangeRateStr,$equivAmountStr,${runningBalance.toPlainString()}\n")
            }
            
            writer.flush()
            writer.close()
            fos.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun triggerShareIntent(context: Context, file: File, customerName: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.csv_share_subject, customerName))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.csv_share_text, customerName))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.csv_share_chooser_title)))
            Toast.makeText(context, context.getString(R.string.habayeb_export_csv_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.habayeb_export_csv_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }
}
