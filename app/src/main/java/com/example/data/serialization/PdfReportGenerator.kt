package com.example.data.serialization

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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

object PdfReportGenerator {

    // Centralized Grid Coordinates
    private const val START_X = 42f
    private const val END_X = 553f
    private const val PRINT_WIDTH = 511f

    private const val COL_SEQ_X = 523f
    private const val COL_SEQ_WIDTH = 30

    private const val COL_DATE_X = 433f
    private const val COL_DATE_WIDTH = 90

    private const val COL_DESC_X = 242f
    private const val COL_DESC_WIDTH = 191

    private const val COL_DEBIT_X = 177f
    private const val COL_DEBIT_WIDTH = 65

    private const val COL_CREDIT_X = 112f
    private const val COL_CREDIT_WIDTH = 65

    private const val COL_BAL_X = 42f
    private const val COL_BAL_WIDTH = 70

    private fun drawArabicText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        paint: Paint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        val textPaint = TextPaint(paint)
        val layout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, textPaint, width, alignment, 1f, 0f, false)
        }
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height
    }

    private fun drawHeaderSection(
        canvas: Canvas,
        state: CustomerReportState,
        context: Context,
        primaryColorHex: String
    ) {
        val paintBizName = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintBizDesc = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintBizPhones = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintLeft1 = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintLeft2 = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Right Column (Business Info - Right-aligned at X = 373f, width = 180)
        val rightColX = 373f
        drawArabicText(canvas, state.businessName, rightColX, 42f, 180, paintBizName, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, state.businessDesc, rightColX, 60f, 180, paintBizDesc, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, state.businessPhones, rightColX, 76f, 180, paintBizPhones, Layout.Alignment.ALIGN_NORMAL)

        // Middle Column (Logo - Centered)
        if (state.logoBitmap != null) {
            val logoX = 297.5f - (state.logoWidth / 2f)
            val logoY = 42f + ((55f - state.logoHeight) / 2f)
            canvas.drawBitmap(state.logoBitmap, logoX, logoY, null)
        }

        // Left Column (Documentation Time - Left-aligned at X = 42f, width = 180)
        drawArabicText(canvas, state.docDateText, START_X, 45f, 180, paintLeft1, Layout.Alignment.ALIGN_OPPOSITE)
        drawArabicText(canvas, state.docTimeText, START_X, 62f, 180, paintLeft2, Layout.Alignment.ALIGN_OPPOSITE)

        // Divider Line under Header
        val paintDivider = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(START_X, 110f, END_X, 110f, paintDivider)

        // Title Area (Y = 130f)
        val paintTitle = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        drawArabicText(
            canvas,
            context.getString(R.string.pdf_statement_title, state.customerName),
            START_X,
            130f,
            PRINT_WIDTH.toInt(),
            paintTitle,
            Layout.Alignment.ALIGN_CENTER
        )
    }

    private fun drawSummarySection(
        canvas: Canvas,
        state: CustomerReportState,
        context: Context,
        primaryColorHex: String
    ) {
        val paintCardBg = Paint().apply {
            color = Color.parseColor("#FBFBFB")
            style = Paint.Style.FILL
        }
        val paintCardBorder = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
        }
        val paintCardLabel = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintCardVal = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintCardValNet = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Card 1 (Right): Debts
        canvas.drawRoundRect(396f, 180f, 553f, 230f, 6f, 6f, paintCardBg)
        canvas.drawRoundRect(396f, 180f, 553f, 230f, 6f, 6f, paintCardBorder)
        drawArabicText(canvas, context.getString(R.string.pdf_card_owed_by_them), 396f, 188f, 157, paintCardLabel, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, state.totalDebtsStr, 396f, 207f, 157, paintCardVal, Layout.Alignment.ALIGN_CENTER)

        // Card 2 (Middle): Payments
        canvas.drawRoundRect(219f, 180f, 376f, 230f, 6f, 6f, paintCardBg)
        canvas.drawRoundRect(219f, 180f, 376f, 230f, 6f, 6f, paintCardBorder)
        drawArabicText(canvas, context.getString(R.string.pdf_card_owed_to_them), 219f, 188f, 157, paintCardLabel, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, state.totalPaymentsStr, 219f, 207f, 157, paintCardVal, Layout.Alignment.ALIGN_CENTER)

        // Card 3 (Left): Net Remaining
        canvas.drawRoundRect(42f, 180f, 199f, 230f, 6f, 6f, paintCardBg)
        canvas.drawRoundRect(42f, 180f, 199f, 230f, 6f, 6f, paintCardBorder)
        drawArabicText(canvas, context.getString(R.string.pdf_card_net_remaining), 42f, 188f, 157, paintCardLabel, Layout.Alignment.ALIGN_CENTER)
        val netText = "${state.netRemainingStr} ${state.netStatus}"
        drawArabicText(canvas, netText, 42f, 207f, 157, paintCardValNet, Layout.Alignment.ALIGN_CENTER)
    }

    private fun drawTableHeader(canvas: Canvas, y: Float, context: Context) {
        val paintHeaderBg = Paint().apply {
            color = Color.parseColor("#F9FAFB")
            style = Paint.Style.FILL
        }
        canvas.drawRect(START_X, y, END_X, y + 30f, paintHeaderBg)

        val paintHeaderBorder = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(START_X, y, END_X, y, paintHeaderBorder)
        canvas.drawLine(START_X, y + 30f, END_X, y + 30f, paintHeaderBorder)

        val paintHeaderText = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        drawArabicText(canvas, context.getString(R.string.pdf_col_m), COL_SEQ_X, y + 9f, COL_SEQ_WIDTH, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_date), COL_DATE_X, y + 9f, COL_DATE_WIDTH, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_description), COL_DESC_X, y + 9f, COL_DESC_WIDTH, paintHeaderText, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, context.getString(R.string.pdf_col_owed_by), COL_DEBIT_X, y + 9f, COL_DEBIT_WIDTH, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_owed_to), COL_CREDIT_X, y + 9f, COL_CREDIT_WIDTH, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
        drawArabicText(canvas, context.getString(R.string.pdf_col_remaining), COL_BAL_X, y + 9f, COL_BAL_WIDTH, paintHeaderText, Layout.Alignment.ALIGN_CENTER)
    }

    private fun drawSubsequentPageHeader(canvas: Canvas, customerName: String, primaryColorHex: String, context: Context) {
        val paintMiniHeader = Paint().apply {
            color = Color.parseColor(primaryColorHex)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val miniHeaderText = context.getString(R.string.pdf_mini_header_text, customerName)
        drawArabicText(canvas, miniHeaderText, START_X, 25f, PRINT_WIDTH.toInt(), paintMiniHeader, Layout.Alignment.ALIGN_NORMAL)

        val paintMiniLine = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(START_X, 38f, END_X, 38f, paintMiniLine)
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int, primaryColorHex: String, context: Context) {
        val paintFooterText = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val footerTextLeft = context.getString(R.string.pdf_footer_page, pageNum, totalPages)
        val footerTextRight = context.getString(R.string.pdf_footer_certified)

        drawArabicText(canvas, footerTextLeft, START_X, 800f, 150, paintFooterText, Layout.Alignment.ALIGN_OPPOSITE)
        drawArabicText(canvas, footerTextRight, 200f, 800f, 353, paintFooterText, Layout.Alignment.ALIGN_NORMAL)
    }

    private fun drawCurrencyTotals(
        canvas: Canvas,
        state: CustomerReportState,
        y: Float,
        context: Context,
        primaryColorHex: String
    ): Float {
        var currentY = y
        val paintTotalsTitle = Paint().apply {
            color = Color.parseColor(primaryColorHex)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        drawArabicText(canvas, context.getString(R.string.pdf_summary_independent_totals), START_X, currentY, PRINT_WIDTH.toInt(), paintTotalsTitle, Layout.Alignment.ALIGN_NORMAL)
        currentY += 25f

        val paintTotalsLabel = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintTotalsVal = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // 1. Primary/Active Currency Total
        drawArabicText(canvas, context.getString(R.string.pdf_total_default_active), 200f, currentY, 353, paintTotalsLabel, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, state.netRemainingStr, START_X, currentY, 150, paintTotalsVal, Layout.Alignment.ALIGN_OPPOSITE)
        currentY += 20f

        // 2. Base Uncalculated Total
        drawArabicText(canvas, context.getString(R.string.pdf_total_uncalculated_default), 200f, currentY, 353, paintTotalsLabel, Layout.Alignment.ALIGN_NORMAL)
        drawArabicText(canvas, state.baseNetStr, START_X, currentY, 150, paintTotalsVal, Layout.Alignment.ALIGN_OPPOSITE)
        currentY += 20f

        // 3. Other Foreign Currency Independent Totals
        if (state.foreignCurrencyTotals.isNotEmpty()) {
            currentY += 10f
            drawArabicText(canvas, context.getString(R.string.pdf_independent_totals_uncalculated), 200f, currentY, 353, paintTotalsLabel, Layout.Alignment.ALIGN_NORMAL)
            currentY += 20f

            for ((currency, sumStr) in state.foreignCurrencyTotals) {
                drawArabicText(canvas, context.getString(R.string.pdf_total_currency_prefix, currency), 200f, currentY, 300, paintTotalsLabel, Layout.Alignment.ALIGN_NORMAL)
                drawArabicText(canvas, sumStr, START_X, currentY, 150, paintTotalsVal, Layout.Alignment.ALIGN_OPPOSITE)
                currentY += 20f
            }
        }

        return currentY
    }

    private fun generatePdfFileInternal(
        context: Context,
        state: CustomerReportState,
        primaryColorHex: String = "#0F4C43"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val totalItems = state.transactions.size
        val totalPages = if (totalItems <= 14) {
            1
        } else {
            1 + ((totalItems - 14 + 19) / 20)
        }

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Draw initial sections on the first page
        drawHeaderSection(canvas, state, context, primaryColorHex)
        drawSummarySection(canvas, state, context, primaryColorHex)
        drawTableHeader(canvas, 250f, context)

        // Predefine Paint configurations to avoid object allocation overhead inside rendering loop
        val paintCellNormal = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintCellBold = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintEmptyDash = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintOwedBg = Paint().apply {
            color = Color.parseColor("#FEF2F2")
            style = Paint.Style.FILL
        }
        val paintOwedText = Paint().apply {
            color = Color.parseColor("#B91C1C")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintPaymentBg = Paint().apply {
            color = Color.parseColor("#F0FDF4")
            style = Paint.Style.FILL
        }
        val paintPaymentText = Paint().apply {
            color = Color.parseColor("#156534")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintDayText = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintDateText = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintRowDivider = Paint().apply {
            color = Color.parseColor("#F3F4F6")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        val paintForeignBg = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val textPaintDesc = TextPaint(paintCellNormal)

        var currentY = 280f
        val rowHeight = 35f

        for (tx in state.transactions) {
            if (currentY + rowHeight > 760f) {
                // Finalize active page
                drawFooter(canvas, currentPageNumber, totalPages, primaryColorHex, context)
                pdfDocument.finishPage(page)

                // Instantiate secondary pages dynamically
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawSubsequentPageHeader(canvas, state.customerName, primaryColorHex, context)

                currentY = 60f
                drawTableHeader(canvas, currentY, context)
                currentY += 30f
            }

            if (tx.isForeign) {
                canvas.drawRect(START_X, currentY, END_X, currentY + rowHeight, paintForeignBg)
            }

            canvas.drawLine(START_X, currentY + rowHeight, END_X, currentY + rowHeight, paintRowDivider)

            // 1. Index No
            drawArabicText(canvas, tx.index.toString(), COL_SEQ_X, currentY + 11f, COL_SEQ_WIDTH, paintCellNormal, Layout.Alignment.ALIGN_CENTER)

            // 2. Chronological Calendar Timestamp
            drawArabicText(canvas, tx.dayName, COL_DATE_X, currentY + 6f, COL_DATE_WIDTH, paintDayText, Layout.Alignment.ALIGN_CENTER)
            drawArabicText(canvas, tx.dateStr, COL_DATE_X, currentY + 19f, COL_DATE_WIDTH, paintDateText, Layout.Alignment.ALIGN_CENTER)

            // 3. Multi-line Description
            val layoutDesc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(tx.description, 0, tx.description.length, textPaintDesc, COL_DESC_WIDTH)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(tx.description, textPaintDesc, COL_DESC_WIDTH, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
            }
            val descYOffset = (rowHeight - layoutDesc.height) / 2f
            canvas.save()
            canvas.translate(COL_DESC_X, currentY + descYOffset)
            layoutDesc.draw(canvas)
            canvas.restore()

            // 4. Financial Categorization Badges (Debit/Owed vs Credit/Payment)
            if (tx.isOwed) {
                val badgeLeft = COL_DEBIT_X + 5f
                val badgeTop = currentY + 8.5f
                val badgeRight = COL_DEBIT_X + COL_DEBIT_WIDTH - 5f
                val badgeBottom = currentY + 26.5f
                canvas.drawRoundRect(badgeLeft, badgeTop, badgeRight, badgeBottom, 4f, 4f, paintOwedBg)
                drawArabicText(canvas, tx.owedAmountStr, COL_DEBIT_X, currentY + 11f, COL_DEBIT_WIDTH, paintOwedText, Layout.Alignment.ALIGN_CENTER)

                drawArabicText(canvas, "-", COL_CREDIT_X, currentY + 11f, COL_CREDIT_WIDTH, paintEmptyDash, Layout.Alignment.ALIGN_CENTER)
            } else {
                drawArabicText(canvas, "-", COL_DEBIT_X, currentY + 11f, COL_DEBIT_WIDTH, paintEmptyDash, Layout.Alignment.ALIGN_CENTER)

                val badgeLeft = COL_CREDIT_X + 5f
                val badgeTop = currentY + 8.5f
                val badgeRight = COL_CREDIT_X + COL_CREDIT_WIDTH - 5f
                val badgeBottom = currentY + 26.5f
                canvas.drawRoundRect(badgeLeft, badgeTop, badgeRight, badgeBottom, 4f, 4f, paintPaymentBg)
                drawArabicText(canvas, tx.paymentAmountStr, COL_CREDIT_X, currentY + 11f, COL_CREDIT_WIDTH, paintPaymentText, Layout.Alignment.ALIGN_CENTER)
            }

            // 5. Computed Running Balance Column
            drawArabicText(canvas, tx.runningBalanceStr, COL_BAL_X, currentY + 11f, COL_BAL_WIDTH, paintCellBold, Layout.Alignment.ALIGN_CENTER)

            currentY += rowHeight
        }

        // Check if independent currencies summary exceeds safe layout margins
        val totalsHeight = 120f + (state.foreignCurrencyTotals.size * 20f)
        if (currentY + totalsHeight > 760f) {
            drawFooter(canvas, currentPageNumber, totalPages, primaryColorHex, context)
            pdfDocument.finishPage(page)

            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            drawSubsequentPageHeader(canvas, state.customerName, primaryColorHex, context)
            currentY = 60f
        }

        currentY += 20f
        drawCurrencyTotals(canvas, state, currentY, context, primaryColorHex)

        // Draw last page footer and save output safely
        drawFooter(canvas, currentPageNumber, totalPages, primaryColorHex, context)
        pdfDocument.finishPage(page)

        // Finalize memory resources explicitly
        try {
            state.logoBitmap?.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fileName = "habayeb_${state.customerName}_${System.currentTimeMillis() % 100000}.pdf"
        val file = File(context.cacheDir, fileName)
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    private fun triggerShareOrViewIntent(context: Context, file: File?, action: String) {
        if (file == null) {
            Toast.makeText(
                context,
                context.getString(R.string.habayeb_toast_pdf_export_failed, context.getString(R.string.csv_error_creating_file)),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            if (action == "SHARE") {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.pdf_chooser_title)))
            } else {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.habayeb_toast_pdf_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    fun generateAndHandleCustomerPdfReport(
        context: Context,
        customer: HabayebCustomer,
        netDebt: Double,
        transactions: List<HabayebTransaction>,
        action: String,
        primaryColorHex: String = "#0F4C43"
    ) {
        val state = CustomerReportState.from(context, customer, transactions)
        val file = generatePdfFileInternal(context, state, primaryColorHex)
        triggerShareOrViewIntent(context, file, action)
    }

    fun generateAndHandleCustomerPdfReportAsync(
        context: Context,
        scope: CoroutineScope,
        customer: HabayebCustomer,
        netDebt: Double,
        transactions: List<HabayebTransaction>,
        action: String,
        primaryColorHex: String = "#0F4C43",
        onFinished: () -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            val state = CustomerReportState.from(context, customer, transactions)
            val file = generatePdfFileInternal(context, state, primaryColorHex)
            withContext(Dispatchers.Main) {
                triggerShareOrViewIntent(context, file, action)
                onFinished()
            }
        }
    }
}
