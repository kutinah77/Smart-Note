package com.example.ui.helper

   import android.content.Context
   import com.example.R
   import java.math.BigDecimal
   import java.text.DecimalFormat
   import java.text.DecimalFormatSymbols
   import java.util.Locale

   object FinanceFormatter {

       fun formatCurrency(context: Context, amount: BigDecimal, symbol: String = ""): String {
           val finalSymbol = symbol.ifEmpty { context.getString(R.string.currency_yer) }
           return try {
               val symbols = DecimalFormatSymbols(Locale.ENGLISH)
               val formatter = DecimalFormat("#,##0", symbols)
               val formatted = formatter.format(amount)
               "$formatted $finalSymbol"
           } catch (e: Exception) {
               val symbols = DecimalFormatSymbols(Locale.ENGLISH)
               val formatter = DecimalFormat("#,##0", symbols)
               val formatted = formatter.format(amount)
               "$formatted $finalSymbol"
           }
       }

       fun formatDoubleCurrency(context: Context, amount: Double, symbol: String = ""): String {
           val finalSymbol = symbol.ifEmpty { context.getString(R.string.currency_yer) }
           val symbols = DecimalFormatSymbols(Locale.ENGLISH)
           val formatter = DecimalFormat("#,##0", symbols)
           val formatted = formatter.format(amount)
           return "$formatted $finalSymbol"
       }
   }
