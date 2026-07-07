package com.example.ui.screens.habayeb.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.HabayebRecurringManager
import com.example.ui.screens.habayeb.utils.RecurringConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringTransactionPopup(
    transaction: HabayebTransaction,
    customerName: String,
    onDismiss: () -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color
) {
    val isDark = com.example.ui.theme.LocalIsDark.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Load existing config if available
    val existingConfigs = remember(transaction.id) { HabayebRecurringManager.getAllConfigs(context) }
    val existingConfig = remember(transaction.id) { existingConfigs.find { it.originalTxId == transaction.id } }

    var frequency by remember { mutableStateOf(existingConfig?.frequency ?: "DAILY") }
    
    // Days of Week: Sunday = 1 to Saturday = 7
    var selectedDaysOfWeek by remember {
        mutableStateOf(existingConfig?.daysOfWeek?.toSet() ?: setOf(Calendar.MONDAY))
    }

    // Days of Month: 1 to 31
    var selectedDaysOfMonth by remember {
        mutableStateOf(existingConfig?.daysOfMonth?.toSet() ?: setOf(1))
    }

    var hour by remember { mutableStateOf(existingConfig?.timeHour ?: 9) }
    var minute by remember { mutableStateOf(existingConfig?.timeMinute ?: 0) }

    var startDateMillis by remember { mutableStateOf(existingConfig?.startDateMillis ?: System.currentTimeMillis()) }
    var endDateMillis by remember {
        mutableStateOf(
            existingConfig?.endDateMillis ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) // 1 month default
        )
    }

    val arabicDaysOfWeek = listOf(
        Triple(Calendar.SATURDAY, context.getString(R.string.day_saturday), "S"),
        Triple(Calendar.SUNDAY, context.getString(R.string.day_sunday), "S"),
        Triple(Calendar.MONDAY, context.getString(R.string.day_monday), "M"),
        Triple(Calendar.TUESDAY, context.getString(R.string.day_tuesday), "T"),
        Triple(Calendar.WEDNESDAY, context.getString(R.string.day_wednesday), "W"),
        Triple(Calendar.THURSDAY, context.getString(R.string.day_thursday), "T"),
        Triple(Calendar.FRIDAY, context.getString(R.string.day_friday), "F")
    )

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .heightIn(max = 600.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Banner with Gradient (Sleeker and narrower)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(activeThemeColor, activeSubColor)
                                )
                            )
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(id = R.string.habayeb_recurring_title),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(id = R.string.habayeb_recurring_desc),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Transaction summary card (Slightly shrunk details)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(id = R.string.habayeb_recurring_original_tx),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = customerName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val formattedAmount = try { String.format(Locale.ENGLISH, "%,.0f", transaction.amount) } catch (e: Exception) { transaction.amount.toString() }
                                    val isPositive = transaction.type == "PAYMENT_BY_THEM" || transaction.type == "OWED_TO_THEM"
                                    val amountColor = if (isPositive) {
                                        if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                    } else {
                                        if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                                    }
                                    
                                    val fallbackCurrency = stringResource(id = R.string.currency_yer)
                                    Text(
                                        text = "$formattedAmount ${transaction.currency_code.ifEmpty { fallbackCurrency }}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = amountColor
                                    )
                                }
                                if (transaction.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📝 " + stringResource(id = R.string.habayeb_col_details) + ": ${transaction.description}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 1. Frequency Selector Tab Row (Much more compact)
                        Text(
                            text = stringResource(id = R.string.habayeb_recurring_interval_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val dailyLabel = stringResource(id = R.string.habayeb_recurring_daily)
                        val weeklyLabel = stringResource(id = R.string.habayeb_recurring_weekly)
                        val monthlyLabel = stringResource(id = R.string.habayeb_recurring_monthly)
                        val options = remember(dailyLabel, weeklyLabel, monthlyLabel) {
                            listOf(
                                "DAILY" to dailyLabel,
                                "WEEKLY" to weeklyLabel,
                                "MONTHLY" to monthlyLabel
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            options.forEach { (key, label) ->
                                val selected = frequency == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) activeThemeColor else Color.Transparent)
                                        .clickable {
                                            frequency = key
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 2. Specific day selector based on frequency
                        Crossfade(targetState = frequency, label = "freqConfig") { targetFreq ->
                            when (targetFreq) {
                                "DAILY" -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(id = R.string.habayeb_recurring_daily_desc),
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                                "WEEKLY" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_recurring_weekly_desc),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            arabicDaysOfWeek.forEach { (dayInt, name, short) ->
                                                val isSelected = selectedDaysOfWeek.contains(dayInt)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) activeThemeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) activeThemeColor else Color(0xFFCBD5E1),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable {
                                                            selectedDaysOfWeek = if (isSelected) {
                                                                if (selectedDaysOfWeek.size > 1) selectedDaysOfWeek - dayInt else selectedDaysOfWeek
                                                            } else {
                                                                selectedDaysOfWeek + dayInt
                                                            }
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = name,
                                                        color = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "MONTHLY" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_recurring_monthly_desc),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            (1..31).forEach { dayNum ->
                                                val isSelected = selectedDaysOfMonth.contains(dayNum)
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant)
                                                        .clickable {
                                                            selectedDaysOfMonth = if (isSelected) {
                                                                if (selectedDaysOfMonth.size > 1) selectedDaysOfMonth - dayNum else selectedDaysOfMonth
                                                            } else {
                                                                selectedDaysOfMonth + dayNum
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Specific Time Section (shrunk)
                        Text(
                            text = stringResource(id = R.string.habayeb_recurring_time_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, selectedHour, selectedMinute ->
                                            hour = selectedHour
                                            minute = selectedMinute
                                        },
                                        hour,
                                        minute,
                                        false
                                    ).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = activeThemeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    val c = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    Text(
                                        text = timeFormatter.format(c.time),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.habayeb_recurring_change_time),
                                    fontSize = 11.sp,
                                    color = activeThemeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 4. Custom Duration (From - To) (shrunk)
                        Text(
                            text = stringResource(id = R.string.habayeb_recurring_validity_period),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Date
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = stringResource(id = R.string.habayeb_recurring_start_date), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val c = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    c.set(y, m, d)
                                                    startDateMillis = c.timeInMillis
                                                },
                                                c.get(Calendar.YEAR),
                                                c.get(Calendar.MONTH),
                                                c.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = dateFormatter.format(Date(startDateMillis)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // End Date
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = stringResource(id = R.string.habayeb_recurring_end_date), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val c = Calendar.getInstance().apply { timeInMillis = endDateMillis }
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    c.set(y, m, d)
                                                    endDateMillis = c.timeInMillis
                                                },
                                                c.get(Calendar.YEAR),
                                                c.get(Calendar.MONTH),
                                                c.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = dateFormatter.format(Date(endDateMillis)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Action Buttons Row (Sleeker and smaller height)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                            ) {
                                Text(text = stringResource(id = R.string.habayeb_cancel), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (startDateMillis > endDateMillis) {
                                        Toast.makeText(context, context.getString(R.string.habayeb_recurring_toast_date_error), Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val newConfig = RecurringConfig(
                                        id = existingConfig?.id ?: "rec_tx_${UUID.randomUUID().toString().take(6)}",
                                        originalTxId = transaction.id,
                                        customerId = transaction.customerId,
                                        customerName = customerName,
                                        amount = transaction.amount,
                                        type = transaction.type,
                                        description = transaction.description,
                                        frequency = frequency,
                                        daysOfWeek = if (frequency == "WEEKLY") selectedDaysOfWeek.toList() else emptyList(),
                                        daysOfMonth = if (frequency == "MONTHLY") selectedDaysOfMonth.toList() else emptyList(),
                                        timeHour = hour,
                                        timeMinute = minute,
                                        startDateMillis = startDateMillis,
                                        endDateMillis = endDateMillis,
                                        lastExecutedTimestamp = existingConfig?.lastExecutedTimestamp ?: 0L,
                                        isActive = true
                                    )

                                    HabayebRecurringManager.saveConfig(context, newConfig)
                                    Toast.makeText(context, context.getString(R.string.habayeb_recurring_toast_schedule_success), Toast.LENGTH_LONG).show()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (existingConfig != null) stringResource(id = R.string.habayeb_recurring_btn_update) else stringResource(id = R.string.habayeb_recurring_btn_activate),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Remove existing config option
                        if (existingConfig != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = R.string.habayeb_recurring_btn_stop),
                                color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HabayebRecurringManager.deleteConfig(context, existingConfig.id)
                                        Toast.makeText(context, context.getString(R.string.habayeb_recurring_toast_stop_success), Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
