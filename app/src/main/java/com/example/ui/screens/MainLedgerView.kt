package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.TransactionDb
import com.example.ui.screens.ledger.components.*
import com.example.ui.screens.ledger.sections.LedgerDialogsWrapper
import com.example.ui.screens.ledger.sections.LedgerListSection
import com.example.ui.screens.ledger.sections.LedgerOverlayManager
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LedgerUiState(
    val showTxDialog: Boolean = false,
    val txDialogType: String = "EXPENSE", // INCOME or EXPENSE
    val editingTransaction: TransactionDb? = null,
    val showActivationDialog: Boolean = false,
    val showCommitmentsListSheet: Boolean = false,
    val reorderCommitmentTarget: FixedCommitment? = null,
    val showCommitmentDialog: Boolean = false,
    val editingCommitment: FixedCommitment? = null,
    val activeDayKey: String? = null,
    val showSearch: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedTxIds: List<String> = emptyList(),
    val collapsedMonths: Set<String> = emptySet(),
    val isHabayebActive: Boolean = false,
    val isDaySelectionMode: Boolean = false,
    val selectedDayKeys: List<String> = emptyList(),
    val showDeleteDaysDialog: Boolean = false,
    val isScreenReady: Boolean = true,
    val habayebButtonCenter: Offset = Offset.Zero
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainLedgerView(
    viewModel: FinanceViewModel,
    settings: AppSettings,
    onBackIntercept: (Boolean) -> Unit, // intercepts back to cancel selection mode if active
    onMenuClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues()
) {
    val bottomPadding = contentPadding.calculateBottomPadding()

    val totalCash by viewModel.totalCashState.collectAsStateWithLifecycle()
    val commitments by viewModel.commitmentsState.collectAsStateWithLifecycle()
    val monthlyLedger by viewModel.monthlyLedgerState.collectAsStateWithLifecycle()
    val ledgerUiState by viewModel.ledgerUiState.collectAsStateWithLifecycle()
    val isScreenReady = !ledgerUiState.isLoading

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val isDark = when (viewModel.settingsState.value.themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false // White text/icons
            insetsController.isAppearanceLightNavigationBars = !isDark // White/transparent look
        }
    }

    val lazyListState = rememberLazyListState()
    val collapseFraction by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val offset = lazyListState.firstVisibleItemScrollOffset.toFloat()
                (offset / 180f).coerceIn(0f, 1f)
            }
        }
    }

    var uiState by remember { mutableStateOf(LedgerUiState()) }

    // Licensing & Activation states
    val deviceId by viewModel.deviceIdState.collectAsStateWithLifecycle()
    val showActivationRequired by viewModel.showActivationRequired.collectAsStateWithLifecycle()

    LaunchedEffect(showActivationRequired) {
        if (showActivationRequired) {
            uiState = uiState.copy(showActivationDialog = true)
            viewModel.showActivationRequired.value = false
        }
    }

    // Reactive active day resolver helper
    val activeDayLedger = remember(uiState.activeDayKey, monthlyLedger) {
        if (uiState.activeDayKey == null) null
        else {
            monthlyLedger.flatMap { ml ->
                ml.days.map { day -> "${ml.monthKey}_${day.dayNumber}" to day }
            }.find { it.first == uiState.activeDayKey }?.second
        }
    }

    BackHandler(enabled = uiState.isSelectionMode || uiState.isDaySelectionMode || uiState.activeDayKey != null || uiState.showSearch || uiState.isHabayebActive) {
        if (uiState.isHabayebActive) {
            uiState = uiState.copy(isHabayebActive = false)
        } else if (uiState.isSelectionMode) {
            uiState = uiState.copy(selectedTxIds = emptyList(), isSelectionMode = false)
        } else if (uiState.isDaySelectionMode) {
            uiState = uiState.copy(selectedDayKeys = emptyList(), isDaySelectionMode = false)
        } else if (uiState.activeDayKey != null) {
            uiState = uiState.copy(activeDayKey = null)
        } else if (uiState.showSearch) {
            uiState = uiState.copy(showSearch = false)
        }
    }

    // Export public clear selection trigger
    fun clearSelection() {
        uiState = uiState.copy(
            selectedTxIds = emptyList(),
            selectedDayKeys = emptyList(),
            isSelectionMode = false,
            isDaySelectionMode = false
        )
    }

    val linkHabayebDebts by viewModel.linkHabayebDebtsState.collectAsStateWithLifecycle()
    val habayebOwedByThemTotalState by viewModel.habayebOwedByThemTotalState.collectAsStateWithLifecycle()
    val habayebOwedByThemTotal = habayebOwedByThemTotalState.toDouble()

    // Precompute commitments coverage details using the UseCase
    val computedCommitments = remember(commitments, totalCash, linkHabayebDebts, habayebOwedByThemTotal) {
        viewModel.calculateCommitmentCoverageUseCase.execute(
            commitments = commitments,
            totalCash = totalCash,
            linkHabayebDebts = linkHabayebDebts,
            habayebOwedByThemTotal = habayebOwedByThemTotal
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // High-fidelity pinned collapsible top header
        PinnedMainLedgerHeader(
            collapseFraction = collapseFraction,
            onMenuClick = onMenuClick,
            onSearchClick = { uiState = uiState.copy(showSearch = true) },
            onHabayebClick = { uiState = uiState.copy(isHabayebActive = true) }
        )

        LedgerListSection(
            lazyListState = lazyListState,
            viewModel = viewModel,
            settings = settings,
            collapseFraction = collapseFraction,
            isDaySelectionMode = uiState.isDaySelectionMode,
            onDaySelectionModeChange = { uiState = uiState.copy(isDaySelectionMode = it) },
            selectedDayKeys = uiState.selectedDayKeys,
            onSelectedDayKeysChange = { uiState = uiState.copy(selectedDayKeys = it) },
            onDeleteSelectedDays = {
                if (uiState.selectedDayKeys.isNotEmpty()) {
                    uiState = uiState.copy(showDeleteDaysDialog = true)
                }
            },
            onMenuClick = onMenuClick,
            onSearchClick = { uiState = uiState.copy(showSearch = true) },
            monthlyLedger = monthlyLedger,
            commitments = commitments,
            computedCommitments = computedCommitments,
            linkHabayebDebts = linkHabayebDebts,
            isScreenReady = isScreenReady,
            collapsedMonths = uiState.collapsedMonths,
            onCollapsedMonthsChange = { uiState = uiState.copy(collapsedMonths = it) },
            isSelectionMode = uiState.isSelectionMode,
            haptic = haptic,
            bottomPadding = bottomPadding,
            onActiveDayKeyChange = { uiState = uiState.copy(activeDayKey = it) }
        )

        // Floating action buttons (Dual floating configuration) - Compressed & modern
        LedgerBottomDock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 12.dp),
            isSelectionMode = uiState.isSelectionMode,
            selectedTxIdsCount = uiState.selectedTxIds.size,
            onDeleteSelectedClick = {
                scope.launch {
                    viewModel.deleteTransactionsBulk(
                        uiState.selectedTxIds,
                        context.getString(R.string.ledger_delete_selected_warning, uiState.selectedTxIds.size)
                    )
                    delay(200)
                    clearSelection()
                }
            },
            onShowCommitmentsClick = { uiState = uiState.copy(showCommitmentsListSheet = true) },
            onAddIncomeClick = {
                uiState = uiState.copy(
                    editingTransaction = null,
                    txDialogType = "INCOME",
                    showTxDialog = true
                )
            },
            onAddExpenseClick = {
                uiState = uiState.copy(
                    editingTransaction = null,
                    txDialogType = "EXPENSE",
                    showTxDialog = true
                )
            }
        )
    }

    // Modal dialogs and wrappers
    LedgerDialogsWrapper(
        viewModel = viewModel,
        settings = settings,
        activeThemeColor = EmeraldPrimary,
        activeSubColor = EmeraldLight,
        showDeleteDaysDialog = uiState.showDeleteDaysDialog,
        onShowDeleteDaysDialogChange = { uiState = uiState.copy(showDeleteDaysDialog = it) },
        selectedDayKeys = uiState.selectedDayKeys,
        onClearSelectedDayKeys = { uiState = uiState.copy(selectedDayKeys = emptyList()) },
        onDaySelectionModeChange = { uiState = uiState.copy(isDaySelectionMode = it) },
        monthlyLedger = monthlyLedger,
        showTxDialog = uiState.showTxDialog,
        onShowTxDialogChange = { uiState = uiState.copy(showTxDialog = it) },
        txDialogType = uiState.txDialogType,
        onTxDialogTypeChange = { uiState = uiState.copy(txDialogType = it) },
        editingTransaction = uiState.editingTransaction,
        onEditingTransactionChange = { uiState = uiState.copy(editingTransaction = it) },
        showSearch = uiState.showSearch,
        onShowSearchChange = { uiState = uiState.copy(showSearch = it) },
        showCommitmentsListSheet = uiState.showCommitmentsListSheet,
        onShowCommitmentsListSheetChange = { uiState = uiState.copy(showCommitmentsListSheet = it) },
        reorderCommitmentTarget = uiState.reorderCommitmentTarget,
        onReorderCommitmentTargetChange = { uiState = uiState.copy(reorderCommitmentTarget = it) },
        showCommitmentDialog = uiState.showCommitmentDialog,
        onShowCommitmentDialogChange = { uiState = uiState.copy(showCommitmentDialog = it) },
        editingCommitment = uiState.editingCommitment,
        onEditingCommitmentChange = { uiState = uiState.copy(editingCommitment = it) },
        commitments = commitments,
        computedCommitments = computedCommitments,
        totalCash = totalCash,
        showActivationDialog = uiState.showActivationDialog,
        onShowActivationDialogChange = { uiState = uiState.copy(showActivationDialog = it) },
        deviceId = deviceId,
        activeDayKey = uiState.activeDayKey,
        onActiveDayKeyChange = { uiState = uiState.copy(activeDayKey = it) },
        activeDayLedger = activeDayLedger
    )

    // Container Transform / Circular Reveal Motion Screen Overlay
    LedgerOverlayManager(
        isHabayebActive = uiState.isHabayebActive,
        onClose = { uiState = uiState.copy(isHabayebActive = false) },
        habayebButtonCenter = uiState.habayebButtonCenter,
        viewModel = viewModel,
        onMenuClick = onMenuClick
    )
}
