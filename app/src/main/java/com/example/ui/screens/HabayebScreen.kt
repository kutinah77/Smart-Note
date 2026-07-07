package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.components.HabayebFilterToolbar
import com.example.ui.screens.habayeb.sections.HabayebActionOrchestrator
import com.example.ui.screens.habayeb.sections.HabayebListSection
import com.example.ui.screens.habayeb.sections.HabayebStatsHeader
import com.example.ui.screens.habayeb.utils.rememberHabayebScrollState
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabayebScreen(
    viewModel: FinanceViewModel,
    onMenuClick: () -> Unit,
    onClose: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val activeThemeColor = MaterialTheme.colorScheme.primary
    val activeSubColor = MaterialTheme.colorScheme.primaryContainer
    val primaryColor = activeThemeColor
    val containerColor = activeSubColor
    val surfaceBackgroundColor = MaterialTheme.colorScheme.background
    
    val isDark = when (viewModel.settingsState.value.themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    // Observe DB lists
    val customersState by viewModel.customersUiState.collectAsStateWithLifecycle()
    val totalOwedByThemState by viewModel.habayebOwedByThemTotalState.collectAsStateWithLifecycle()
    val totalOwedToThemState by viewModel.habayebOwedToThemTotalState.collectAsStateWithLifecycle()
    val totalOwedByThem = totalOwedByThemState.toDouble()
    val totalOwedToThem = totalOwedToThemState.toDouble()
    val currencySymbol = viewModel.settingsState.collectAsStateWithLifecycle().value.currencySymbol
    val isPrivacyModeState = viewModel.isPrivacyModeEnabled.collectAsStateWithLifecycle()

    // UI filters
    var selectedFilterTab by rememberSaveable { mutableStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    // Multi-Select state
    val selectedCustomerIds = remember { mutableStateListOf<String>() }
    val temporarilyHiddenCustomerIds = remember { mutableStateListOf<String>() }
    var isMultiSelectActive by rememberSaveable { mutableStateOf(false) }

    // Dialog sheets states
    var showAddCustomerDialog by rememberSaveable { mutableStateOf(false) }
    var activeCustomerForHistory by remember { mutableStateOf<HabayebCustomer?>(null) }
    var stableCustomer by remember { mutableStateOf<HabayebCustomer?>(null) }
    var showAddTransactionDialogForCustomer by remember { mutableStateOf<HabayebCustomer?>(null) }
    var defaultTransactionTypeForDialog by rememberSaveable { mutableStateOf("OWED_BY_THEM") }
    var editingTransactionForDialog by remember { mutableStateOf<HabayebTransaction?>(null) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showEditCustomerDialog by rememberSaveable { mutableStateOf(false) }
    var editingCustomerForDialog by remember { mutableStateOf<HabayebCustomer?>(null) }
    var financialSortMode by rememberSaveable { mutableStateOf(0) }
    var historicalSortMode by rememberSaveable { mutableStateOf(1) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Collapsible header logic encapsulated in HabayebScrollState
    val scrollState = rememberHabayebScrollState()

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            scrollState.reset()
        }
    }

    // Back handler: dismisses overlays of selection first
    BackHandler {
        if (isMultiSelectActive) {
            selectedCustomerIds.clear()
            isMultiSelectActive = false
        } else if (isSearchActive) {
            searchQuery = ""
            isSearchActive = false
        } else if (activeCustomerForHistory != null) {
            activeCustomerForHistory = null
        } else {
            onClose()
        }
    }

    // Use FilterHabayebCustomersUseCase from ViewModel
    val filteredCustomers = remember(customersState, selectedFilterTab, searchQuery, financialSortMode, historicalSortMode, temporarilyHiddenCustomerIds.toList()) {
        viewModel.filterHabayebCustomersUseCase.execute(
            customers = customersState.customers,
            searchQuery = searchQuery,
            selectedFilterTab = selectedFilterTab,
            financialSortMode = financialSortMode,
            historicalSortMode = historicalSortMode,
            temporarilyHiddenCustomerIds = temporarilyHiddenCustomerIds
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceBackgroundColor)
                .testTag("habayeb_screen_root")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top header stats and filter section
                HabayebStatsHeader(
                    isSearchActive = isSearchActive,
                    onSearchActiveChanged = { isSearchActive = it },
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    onMenuClick = onMenuClick,
                    haptic = haptic,
                    totalOwedByThem = totalOwedByThem,
                    totalOwedToThem = totalOwedToThem,
                    isPrivacyMode = isPrivacyModeState.value,
                    onTogglePrivacy = { viewModel.togglePrivacyMode() },
                    currencySymbol = currencySymbol,
                    selectedFilterTab = selectedFilterTab,
                    onFilterTabSelected = { selectedFilterTab = it },
                    activeThemeColor = activeThemeColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Sort tools
                HabayebFilterToolbar(
                    filteredCustomersCount = filteredCustomers.size,
                    financialSortMode = financialSortMode,
                    onFinancialSortModeChanged = { financialSortMode = it },
                    historicalSortMode = historicalSortMode,
                    onHistoricalSortModeChanged = { historicalSortMode = it },
                    activeThemeColor = activeThemeColor,
                    activeSubColor = activeSubColor,
                    haptic = haptic,
                    onScrollToTop = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                // Main customer list and selection section
                HabayebListSection(
                    filteredCustomers = filteredCustomers,
                    isScreenReady = !customersState.isLoading,
                    isPrivacyMode = isPrivacyModeState.value,
                    isMultiSelectActive = isMultiSelectActive,
                    onMultiSelectActiveChanged = { isMultiSelectActive = it },
                    selectedCustomerIds = selectedCustomerIds,
                    onSelectedCustomerIdsChanged = { newList ->
                        selectedCustomerIds.clear()
                        selectedCustomerIds.addAll(newList)
                    },
                    activeThemeColor = activeThemeColor,
                    activeSubColor = activeSubColor,
                    currencySymbol = currencySymbol,
                    haptic = haptic,
                    listState = listState,
                    nestedScrollConnection = scrollState.nestedScrollConnection(isSearchActive),
                    selectedFilterTab = selectedFilterTab,
                    contentPadding = contentPadding,
                    onCustomerClick = { customer ->
                        activeCustomerForHistory = customer.originalCustomer
                    },
                    onQuickAdd = { customer ->
                        defaultTransactionTypeForDialog = if (customer.netDebt >= 0.0) "OWED_BY_THEM" else "OWED_TO_THEM"
                        showAddTransactionDialogForCustomer = customer.originalCustomer
                    },
                    onDeleteSelectedClick = {
                        showDeleteConfirmDialog = true
                    }
                )
            }

            // Add Customer FAB
            if (!isMultiSelectActive && activeCustomerForHistory == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp, start = 16.dp)
                        .size(58.dp)
                        .shadow(10.dp, CircleShape, spotColor = primaryColor.copy(alpha = 0.6f))
                        .background(primaryColor, CircleShape)
                        .border(1.dp, containerColor.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAddCustomerDialog = true
                        }
                        .testTag("add_customer_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.habayeb_add_customer_fab),
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
            }

            // Coordinating and orchestrating overlay dialogs
            HabayebActionOrchestrator(
                showAddCustomerDialog = showAddCustomerDialog,
                onShowAddCustomerDialogChanged = { showAddCustomerDialog = it },
                activeCustomerForHistory = activeCustomerForHistory,
                onActiveCustomerForHistoryChanged = { activeCustomerForHistory = it },
                stableCustomer = stableCustomer,
                onStableCustomerChanged = { stableCustomer = it },
                showAddTransactionDialogForCustomer = showAddTransactionDialogForCustomer,
                onShowAddTransactionDialogForCustomerChanged = { showAddTransactionDialogForCustomer = it },
                defaultTransactionTypeForDialog = defaultTransactionTypeForDialog,
                onDefaultTransactionTypeForDialogChanged = { defaultTransactionTypeForDialog = it },
                editingTransactionForDialog = editingTransactionForDialog,
                onEditingTransactionForDialogChanged = { editingTransactionForDialog = it },
                showDeleteConfirmDialog = showDeleteConfirmDialog,
                onShowDeleteConfirmDialogChanged = { showDeleteConfirmDialog = it },
                showEditCustomerDialog = showEditCustomerDialog,
                onShowEditCustomerDialogChanged = { showEditCustomerDialog = it },
                editingCustomerForDialog = editingCustomerForDialog,
                onEditingCustomerForDialogChanged = { editingCustomerForDialog = it },
                selectedCustomerIds = selectedCustomerIds.toList(),
                onSelectedCustomerIdsCleared = { selectedCustomerIds.clear() },
                onMultiSelectActiveChanged = { isMultiSelectActive = it },
                viewModel = viewModel,
                activeThemeColor = activeThemeColor,
                activeSubColor = activeSubColor,
                currencySymbol = currencySymbol,
                contentPadding = contentPadding,
                filteredCustomers = filteredCustomers,
                listState = listState
            )
        }
    }
}
