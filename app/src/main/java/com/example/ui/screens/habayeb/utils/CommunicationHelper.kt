package com.example.ui.screens.habayeb.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.helper.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommunicationHelper {

    fun sendSmsStatement(context: Context, customer: HabayebCustomer, debt: Double, currencySymbol: String) {
        val body = buildStatementMessage(context, customer, debt, currencySymbol)
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${customer.phone}")
                putExtra("sms_body", body)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_statement_send)))
        }
    }

    fun sendWhatsAppStatement(context: Context, customer: HabayebCustomer, debt: Double, currencySymbol: String) {
        val body = buildStatementMessage(context, customer, debt, currencySymbol)
        try {
            val waUrl = "https://wa.me/${customer.phone.replace("+", "").replace(" ", "")}?text=${Uri.encode(body)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_statement_send_whatsapp)))
        }
    }

    fun sendSingleTxSms(context: Context, tx: HabayebTransaction, customer: HabayebCustomer, netDebt: Double, currencySymbol: String) {
        val body = buildSingleTxSmsMessage(context, tx, customer, netDebt, currencySymbol)
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${customer.phone}")
                putExtra("sms_body", body)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_tx_send_notice)))
        }
    }

    fun sendSingleTxWhatsApp(context: Context, tx: HabayebTransaction, customer: HabayebCustomer, netDebt: Double, currencySymbol: String) {
        val body = buildSingleTxWhatsAppMessage(context, tx, customer, netDebt, currencySymbol)
        try {
            val waUrl = "https://wa.me/${customer.phone.replace("+", "").replace(" ", "")}?text=${Uri.encode(body)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_tx_whatsapp_choose)))
        }
    }

    private fun buildStatementMessage(context: Context, customer: HabayebCustomer, debt: Double, currencySymbol: String): String {
        val debtStatus = when {
            debt > 0.0 -> context.getString(R.string.habayeb_statement_status_owed, formatCurrency(kotlin.math.abs(debt), currencySymbol))
            debt < 0.0 -> context.getString(R.string.habayeb_statement_status_to_them, formatCurrency(kotlin.math.abs(debt), currencySymbol))
            else -> context.getString(R.string.habayeb_statement_status_balanced)
        }
        return context.getString(R.string.habayeb_statement_title, customer.name) +
                debtStatus +
                context.getString(R.string.habayeb_statement_thanks)
    }

    private fun buildSingleTxSmsMessage(context: Context, tx: HabayebTransaction, customer: HabayebCustomer, netDebt: Double, currencySymbol: String): String {
        val txTypeAr = getLocalizedTxType(context, tx.type)
        val dateStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date(tx.timestamp * 1000))
        val amountStr = if (tx.is_foreign) {
            "${tx.foreign_amount} ${tx.currency_code}" + if (tx.is_rate_calculated) context.getString(R.string.habayeb_equivalent_val, formatCurrency(tx.equivalent_amount, currencySymbol), tx.exchange_rate.toString()) else ""
        } else {
            formatCurrency(tx.amount, currencySymbol)
        }
        val currentDebtStatusStr = if (netDebt > 0) context.getString(R.string.habayeb_owed) else if (netDebt < 0) context.getString(R.string.habayeb_to_them) else context.getString(R.string.habayeb_balanced)
        return context.getString(R.string.habayeb_tx_sms_notice) +
                context.getString(R.string.habayeb_tx_client, customer.name) +
                context.getString(R.string.habayeb_tx_type_label, txTypeAr) +
                context.getString(R.string.habayeb_tx_amount_label, amountStr) +
                context.getString(R.string.habayeb_tx_details_label, tx.description.ifEmpty { context.getString(R.string.habayeb_no_notes) }) +
                context.getString(R.string.habayeb_tx_date_label, dateStr) +
                context.getString(R.string.habayeb_tx_total_bal, formatCurrency(kotlin.math.abs(netDebt), currencySymbol), currentDebtStatusStr)
    }

    private fun buildSingleTxWhatsAppMessage(context: Context, tx: HabayebTransaction, customer: HabayebCustomer, netDebt: Double, currencySymbol: String): String {
        val txTypeAr = getLocalizedTxType(context, tx.type)
        val dateStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date(tx.timestamp * 1000))
        val amountStr = if (tx.is_foreign) {
            "${tx.foreign_amount} ${tx.currency_code}" + if (tx.is_rate_calculated) context.getString(R.string.habayeb_equivalent_val, formatCurrency(tx.equivalent_amount, currencySymbol), tx.exchange_rate.toString()) else ""
        } else {
            formatCurrency(tx.amount, currencySymbol)
        }
        val currentDebtStatusStr = if (netDebt > 0) context.getString(R.string.habayeb_owed) else if (netDebt < 0) context.getString(R.string.habayeb_to_them) else context.getString(R.string.habayeb_balanced)
        return context.getString(R.string.habayeb_tx_whatsapp_notice) +
                context.getString(R.string.habayeb_tx_client_bold, customer.name) +
                context.getString(R.string.habayeb_tx_type_label_bold, txTypeAr) +
                context.getString(R.string.habayeb_tx_amount_label_bold, amountStr) +
                context.getString(R.string.habayeb_tx_details_label_bold, tx.description.ifEmpty { context.getString(R.string.habayeb_no_notes) }) +
                context.getString(R.string.habayeb_tx_date_label_bold, dateStr) +
                context.getString(R.string.habayeb_tx_total_bal_bold, formatCurrency(kotlin.math.abs(netDebt), currencySymbol), currentDebtStatusStr)
    }

    private fun getLocalizedTxType(context: Context, type: String): String {
        return when (type) {
            "OWED_BY_THEM" -> context.getString(R.string.habayeb_tx_type_owed_by)
            "PAYMENT_BY_THEM" -> context.getString(R.string.habayeb_tx_type_payment_by)
            "OWED_TO_THEM" -> context.getString(R.string.habayeb_tx_type_owed_to)
            "PAYMENT_TO_THEM" -> context.getString(R.string.habayeb_tx_type_payment_to)
            else -> context.getString(R.string.habayeb_tx_type_generic_move)
        }
    }
}
