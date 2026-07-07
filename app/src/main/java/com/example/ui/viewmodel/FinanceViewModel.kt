package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import com.example.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.local.*
import com.example.data.local.entities.*
import com.example.data.repository.AppPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.usecase.*
import com.example.ui.helper.FinanceFormatter
import com.example.ui.state.FinanceUiState
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.util.UUID

sealed class UiEvent {
    data class ShowToast(val messageRes: Int, val isLong: Boolean = false) : UiEvent()
    data class ShareFile(val file: File) : UiEvent()
    data class OpenGoogleDriveApp(val appId: String = "com.google.android.apps.docs") : UiEvent()
}

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinanceRepository(AppDatabase.getDatabase(application), application)
    private val trashDao = AppDatabase.getDatabase(application).trashDao()
    private val backupService = BackupService(application, repository)
    private val appPreferencesRepository = AppPreferencesRepository(application)
    private val businessProfileRepository = com.example.data.repository.BusinessProfileRepository(application)

    private val _businessProfile = MutableStateFlow(businessProfileRepository.getProfile())
    val businessProfile: StateFlow<com.example.data.repository.BusinessProfile> = _businessProfile.asStateFlow()

    fun saveBusinessProfile(profile: com.example.data.repository.BusinessProfile) {
        businessProfileRepository.saveProfile(profile)
        _businessProfile.value = profile
    }

    fun refreshBusinessProfile() {
        _businessProfile.value = businessProfileRepository.getProfile()
    }
    
    // Decoupled UseCases
    private val calculateLedgerUseCase = CalculateLedgerUseCase()
    private val habayebBusinessLogicUseCase = HabayebBusinessLogicUseCase()
    private val licenseValidationUseCase = LicenseValidationUseCase()
    private val searchTransactionsUseCase = SearchTransactionsUseCase()
    private val saveHabayebCustomerUseCase = SaveHabayebCustomerUseCase(repository)
    val createHabayebCustomerUseCase = CreateHabayebCustomerUseCase(saveHabayebCustomerUseCase)
    val calculateCommitmentCoverageUseCase = com.example.domain.usecase.CalculateCommitmentCoverageUseCase()
    private val addHabayebTransactionUseCase = AddHabayebTransactionUseCase(repository)
    private val deleteHabayebCustomerUseCase = DeleteHabayebCustomerUseCase(repository)
    private val deleteHabayebTransactionUseCase = DeleteHabayebTransactionUseCase(repository)
    val processHabayebTransactionUseCase = com.example.domain.usecase.ProcessHabayebTransactionUseCase(repository)
    val filterHabayebCustomersUseCase = com.example.domain.usecase.FilterHabayebCustomersUseCase()

    private val _uiEventChannel = kotlinx.coroutines.channels.Channel<UiEvent>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private fun sendUiEvent(event: UiEvent) = viewModelScope.launch { _uiEventChannel.send(event) }

    val googleDriveSyncHelper: CloudSyncProvider = GoogleDriveSyncHelper(application)
    val googleDriveSyncState: StateFlow<CloudSyncState> = googleDriveSyncHelper.syncState

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    // Consolidated mappings from single source of truth uiState
    val cloudBackupsList: StateFlow<List<CloudBackupFile>> = _uiState.map { it.cloudBackupsList }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val isFetchingCloudBackups: StateFlow<Boolean> = _uiState.map { it.isFetchingCloudBackups }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val localBackups: StateFlow<List<File>> = _uiState.map { it.localBackups }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val isPrivacyModeEnabled: StateFlow<Boolean> = _uiState.map { it.isPrivacyModeEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val linkHabayebDebtsState: StateFlow<Boolean> = _uiState.map { it.linkHabayebDebts }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val searchQuery: StateFlow<String> = _uiState.map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _activationTrigger = MutableStateFlow(0)
    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "m_act_code") {
            _activationTrigger.value += 1
            viewModelScope.launch {
                licenseValidationUseCase.verifyActivation(getOrGenerateUnifiedDeviceId(getApplication()), appPreferencesRepository.getActivationCode())
                _activationTrigger.value += 1
            }
        }
    }

    init {
        appPreferencesRepository.registerOnSharedPreferenceChangeListener(preferenceListener)
        viewModelScope.launch(Dispatchers.IO) {
            val shouldPopulate = !appPreferencesRepository.isCategoriesPopulated()
            repository.populateDefaultCategoriesIfNeeded(shouldPopulate, application.applicationContext)
            if (shouldPopulate) appPreferencesRepository.setCategoriesPopulated(true)
            
            _uiState.update { 
                it.copy(
                    tabOrder = appPreferencesRepository.tabOrderFlow.firstOrNull() ?: NavigationPreferences.DEFAULT_ORDER,
                    defaultStartDestination = appPreferencesRepository.defaultStartFlow.firstOrNull() ?: NavigationPreferences.DEFAULT_START,
                    linkHabayebDebts = appPreferencesRepository.isLinkHabayebDebtsEnabled(),
                    isPrivacyModeEnabled = _uiState.value.isPrivacyModeEnabled
                )
            }
            refreshLocalBackups()
        }
    }

    val tabOrderState: StateFlow<String> = appPreferencesRepository.tabOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationPreferences.DEFAULT_ORDER)
    val defaultStartDestinationState: StateFlow<String> = appPreferencesRepository.defaultStartFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationPreferences.DEFAULT_START)

    fun saveTabOrder(order: String) = viewModelScope.launch { appPreferencesRepository.saveTabOrder(order) }
    fun saveDefaultStart(start: String) = viewModelScope.launch { appPreferencesRepository.saveDefaultStart(start) }

    val isSettingsLoaded = MutableStateFlow(false)
    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .onEach { isSettingsLoaded.value = true }
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val commitmentsState: StateFlow<List<FixedCommitment>> = repository.commitmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactionsState: StateFlow<List<TransactionDb>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customCategoriesState: StateFlow<List<CustomCategory>> = repository.customCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedItemsFlow: Flow<List<DeletedItemEntity>> = repository.deletedItemsFlow

    fun getOrGenerateUnifiedDeviceId(context: Context): String = com.example.domain.LicenseManager.getOrGenerateUnifiedDeviceId(context)
    val showActivationRequired = MutableStateFlow(false)
    fun togglePrivacyMode() = _uiState.update { it.copy(isPrivacyModeEnabled = !it.isPrivacyModeEnabled) }

    val deviceIdState: StateFlow<String> = flow {
        emit(getOrGenerateUnifiedDeviceId(getApplication()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getOrGenerateUnifiedDeviceId(getApplication()))

    val isActivatedState: StateFlow<Boolean> = combine(deviceIdState, _activationTrigger) { deviceId, _ ->
        val enteredCode = appPreferencesRepository.getActivationCode()
        if (enteredCode.isBlank()) false else licenseValidationUseCase.verifyActivation(deviceId, enteredCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun activateLicense(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        val deviceId = getOrGenerateUnifiedDeviceId(getApplication())
        val isValid = licenseValidationUseCase.verifyActivation(deviceId, cleanCode)
        if (isValid) {
            appPreferencesRepository.saveActivationCode(cleanCode, licenseValidationUseCase.isPermanentCode(cleanCode))
            _activationTrigger.value += 1
        }
        return isValid
    }

    fun isTrialExpired(): Boolean = licenseValidationUseCase.isTrialExpired(totalTransactionsCount.value, isActivatedState.value)

    val habayebCustomersState: StateFlow<List<HabayebCustomer>> = repository.habayebCustomersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habayebTransactionsState: StateFlow<List<HabayebTransaction>> = repository.habayebTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTransactionsCount: StateFlow<Int> = combine(transactionsState, habayebTransactionsState) { main, habayeb ->
        main.size + habayeb.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleLinkHabayebDebts(enabled: Boolean) {
        appPreferencesRepository.setLinkHabayebDebtsEnabled(enabled)
        _uiState.update { it.copy(linkHabayebDebts = enabled) }
    }

    fun hasShownOnboarding(): Boolean = appPreferencesRepository.hasShownOnboarding()
    fun markOnboardingShown() = appPreferencesRepository.setOnboardingShown(true)

    val customersUiState: StateFlow<com.example.ui.state.CustomersUiState> = combine(
        habayebCustomersState, habayebTransactionsState, settingsState
    ) { customers, transactions, settings ->
        habayebBusinessLogicUseCase.execute(customers, transactions, settings)
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.state.CustomersUiState())

    val habayebOwedByThemTotalState: StateFlow<BigDecimal> = customersUiState.map { it.totalOwedByThem }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)
    val habayebOwedToThemTotalState: StateFlow<BigDecimal> = customersUiState.map { it.totalOwedToThem }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    suspend fun saveHabayebCustomerSuspended(
        customer: HabayebCustomer, initialAmount: Double, initialType: String,
        customTimestamp: Long = System.currentTimeMillis() / 1000, initialDetails: String = "",
        isForeign: Boolean = false, currencyCode: String = "DEFAULT", foreignAmount: Double = 0.0,
        exchangeRate: Double = 1.0, isRateCalculated: Boolean = false, equivalentAmount: Double = 0.0
    ): Boolean = withContext(Dispatchers.IO) {
        if (initialAmount > 0.0 && isTrialExpired()) {
            showActivationRequired.value = true
            return@withContext false
        }
        val success = saveHabayebCustomerUseCase.execute(
            customer, initialAmount, initialType, customTimestamp, initialDetails,
            isForeign, currencyCode, foreignAmount, exchangeRate, isRateCalculated, equivalentAmount
        )
        if (success) {
            triggerSilentLocalBackup()
        } else {
            sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed))
        }
        success
    }

    suspend fun addHabayebTransactionSuspended(
        customerId: String, type: String, amount: Double, desc: String,
        timestamp: Long = System.currentTimeMillis() / 1000, linkedMainTxId: String? = null,
        isForeign: Boolean = false, currencyCode: String = "DEFAULT", foreignAmount: Double = 0.0,
        exchangeRate: Double = 1.0, isRateCalculated: Boolean = false, equivalentAmount: Double = 0.0
    ): Boolean = withContext(Dispatchers.IO) {
        if (isTrialExpired()) {
            showActivationRequired.value = true
            return@withContext false
        }
        val success = addHabayebTransactionUseCase.execute(
            customerId, type, amount, desc, timestamp, linkedMainTxId,
            isForeign, currencyCode, foreignAmount, exchangeRate, isRateCalculated, equivalentAmount
        )
        if (success) {
            triggerSilentLocalBackup()
        } else {
            sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed))
        }
        success
    }

    suspend fun processHabayebTransactionSuspended(
        customerId: String,
        type: String,
        amountStr: String,
        descStr: String,
        dateMillis: Long,
        editingTransaction: com.example.data.local.entities.HabayebTransaction?,
        selectedTransactionCurrency: String,
        currencySymbol: String,
        applyExchangeRate: Boolean,
        settingsRate: Double
    ): com.example.domain.usecase.ProcessHabayebTransactionUseCase.ProcessResult = withContext(Dispatchers.IO) {
        if (isTrialExpired()) {
            showActivationRequired.value = true
            return@withContext com.example.domain.usecase.ProcessHabayebTransactionUseCase.ProcessResult.Error("Trial expired")
        }
        val result = processHabayebTransactionUseCase.execute(
            customerId, type, amountStr, descStr, dateMillis,
            editingTransaction, selectedTransactionCurrency, currencySymbol,
            applyExchangeRate, settingsRate
        )
        if (result is com.example.domain.usecase.ProcessHabayebTransactionUseCase.ProcessResult.Success) {
            triggerSilentLocalBackup()
        }
        result
    }

    fun updateTransactionExchangeRate(txId: String, newRate: Double, calculateRate: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val tx = repository.getHabayebTransactionById(txId) ?: return@launch
            val finalRate = if (newRate <= 0.0) 1.0 else newRate
            val finalEquivalent = tx.foreign_amount * finalRate
            
            if (tx.linkedMainTxId != null) {
                repository.getTransactionById(tx.linkedMainTxId)?.let {
                    repository.saveTransaction(it.copy(amount = if (calculateRate) finalEquivalent else 0.0))
                }
            }
            repository.insertHabayebTransaction(tx.copy(
                exchange_rate = finalRate,
                is_rate_calculated = calculateRate,
                equivalent_amount = if (calculateRate) finalEquivalent else 0.0,
                amount = if (calculateRate) finalEquivalent else tx.foreign_amount
            ))
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun updateHabayebCustomerNameSuspended(customerId: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try { repository.updateCustomerName(customerId, newName); true } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed)); false }
    }

    suspend fun updateHabayebCustomerSuspended(customer: HabayebCustomer): Boolean = withContext(Dispatchers.IO) {
        try { repository.updateCustomer(customer); true } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed)); false }
    }

    fun updateHabayebCustomer(customer: HabayebCustomer) = viewModelScope.launch { updateHabayebCustomerSuspended(customer) }

    suspend fun deleteHabayebCustomerSuspended(customerId: String): Boolean = withContext(Dispatchers.IO) {
        val success = deleteHabayebCustomerUseCase.executeSingle(customerId, habayebCustomersState.value)
        if (!success) {
            sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
        }
        success
    }

    fun deleteHabayebCustomer(customerId: String) = viewModelScope.launch { deleteHabayebCustomerSuspended(customerId) }

    suspend fun deleteMultipleHabayebCustomersSuspended(customerIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        val success = deleteHabayebCustomerUseCase.executeMultiple(customerIds, habayebCustomersState.value)
        if (!success) {
            sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
        }
        success
    }

    suspend fun deleteHabayebTransactionSuspended(txId: String): Boolean = withContext(Dispatchers.IO) {
        val success = deleteHabayebTransactionUseCase.execute(txId, transactionsState.value)
        if (!success) {
            sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed))
        }
        success
    }

    fun deleteHabayebTransaction(txId: String) = viewModelScope.launch { deleteHabayebTransactionSuspended(txId) }

    fun updateSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    val searchResultsState: StateFlow<List<TransactionDb>> = combine(transactionsState, searchQuery) { transactions, query ->
        searchTransactionsUseCase.execute(transactions, query, getApplication())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun calculateSumByType(transactions: List<TransactionDb>, type: String): BigDecimal = calculateLedgerUseCase.calculateSumByType(transactions, type)

    val totalCashState: StateFlow<BigDecimal> = transactionsState
        .map { calculateLedgerUseCase.calculateTotalCash(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    val dailyExpenseComparisonState: StateFlow<Pair<BigDecimal, BigDecimal>> = transactionsState
        .map { calculateLedgerUseCase.calculateDailyExpenseComparison(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(BigDecimal.ZERO, BigDecimal.ZERO))

    val ledgerUiState: StateFlow<com.example.ui.state.MainLedgerUiState> = combine(
        searchResultsState, totalCashState, searchQuery
    ) { txList, totalCash, query ->
        com.example.ui.state.MainLedgerUiState(transactions = txList, totalCash = totalCash.toDouble(), isSearching = query.isNotBlank(), isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.state.MainLedgerUiState())

    val monthlyLedgerState: StateFlow<List<MonthLedger>> = transactionsState
        .map { calculateLedgerUseCase.execute(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencyMigrationService = com.example.domain.service.CurrencyMigrationService(repository)

    fun saveSettings(settings: AppSettings) = viewModelScope.launch(Dispatchers.IO) { repository.saveSettings(settings) }

    fun migrateBaseCurrency(newSymbol: String, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        val currentSettings = settingsState.value
        val oldSymbol = currentSettings.currencySymbol
        if (oldSymbol != newSymbol) {
            val updatedRatesJson = currencyMigrationService.migrateBaseCurrency(
                getApplication(),
                oldSymbol,
                newSymbol,
                currentSettings.exchangeRatesJson
            )
            repository.saveSettings(currentSettings.copy(
                currencySymbol = newSymbol,
                exchangeRatesJson = updatedRatesJson
            ))
        }
        withContext(Dispatchers.Main) {
            onComplete()
        }
    }

    fun verifyCredentials(input: String): Boolean {
        val hashed = com.example.domain.HashUtils.hashString(input.trim())
        val settings = settingsState.value
        return (settings.passcodeHash != null && com.example.domain.DatabaseSecurityGuard.secureEqual(hashed, settings.passcodeHash)) || 
               (settings.recoveryPhraseHash != null && com.example.domain.DatabaseSecurityGuard.secureEqual(hashed, settings.recoveryPhraseHash))
    }

    fun addTransaction(type: String, category: String, amount: Double, description: String, timestamp: Long = System.currentTimeMillis() / 1000, presetId: String? = null) {
        if (isTrialExpired()) { showActivationRequired.value = true; return }
        viewModelScope.launch(Dispatchers.IO) {
            val id = presetId ?: "tx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            repository.saveTransaction(TransactionDb(id = id, timestamp = timestamp, type = type, category = category, amount = amount, description = description))
        }
    }

    fun permanentlyDeleteDeletedItem(item: DeletedItemEntity) = viewModelScope.launch(Dispatchers.IO) { repository.removeDeletedItem(item) }
    fun permanentlyDeleteMultipleItems(items: List<DeletedItemEntity>) = viewModelScope.launch(Dispatchers.IO) { items.forEach { repository.removeDeletedItem(it) } }

    fun restoreMultipleItems(items: List<DeletedItemEntity>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            AppDatabase.getDatabase(getApplication()).withTransaction {
                for (item in items) trashDao.restoreDeletedItem(item)
            }
            withContext(Dispatchers.Main) { android.widget.Toast.makeText(getApplication(), R.string.toast_restore_success, android.widget.Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { android.widget.Toast.makeText(getApplication(), R.string.toast_operation_failed, android.widget.Toast.LENGTH_SHORT).show() }
        }
    }

    fun restoreDeletedItem(item: DeletedItemEntity) = restoreMultipleItems(listOf(item))
    fun emptyTrash() = viewModelScope.launch(Dispatchers.IO) { repository.clearDeletedItems() }

    fun deleteTransaction(tx: TransactionDb) = viewModelScope.launch(Dispatchers.IO) {
        try { repository.softDeleteTransactionToTrash(tx); repository.deleteTransaction(tx) } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed)) }
    }

    fun deleteTransactionById(id: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            transactionsState.value.find { it.id == id }?.let { repository.softDeleteTransactionToTrash(it) }
            repository.deleteTransactionById(id)
        } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed)) }
    }

    fun deleteTransactionsBulk(ids: List<String>, bundleTitle: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val toDelete = transactionsState.value.filter { ids.contains(it.id) }
            if (toDelete.isNotEmpty()) {
                repository.softDeleteTransactionBundleToTrash(toDelete, bundleTitle)
                toDelete.forEach { repository.deleteTransactionById(it.id) }
            }
        } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed)) }
    }

    fun updateTransaction(tx: TransactionDb) = viewModelScope.launch(Dispatchers.IO) {
        try { repository.saveTransaction(tx) } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_operation_failed)) }
    }

    fun saveCommitment(name: String, targetAmount: Double, currentProgress: Double) = viewModelScope.launch(Dispatchers.IO) {
        try { repository.saveCommitment(FixedCommitment(name, targetAmount, currentProgress, commitmentsState.value.size)) } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed)) }
    }

    fun updateCommitmentDirectly(commitment: FixedCommitment) = viewModelScope.launch(Dispatchers.IO) {
        try { repository.saveCommitment(commitment) } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_operation_failed)) }
    }

    fun reorderCommitment(commitment: FixedCommitment, toPosition: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val currentList = commitmentsState.value.toMutableList().apply { sortBy { it.orderIndex } }
            val targetIndex = (toPosition - 1).coerceIn(0, currentList.size - 1)
            val currentIndex = currentList.indexOfFirst { it.name == commitment.name }
            if (currentIndex != -1 && currentIndex != targetIndex) {
                currentList.add(targetIndex, currentList.removeAt(currentIndex))
                repository.updateCommitments(currentList.mapIndexed { index, fc -> fc.copy(orderIndex = index) })
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun deleteCommitment(name: String) = viewModelScope.launch(Dispatchers.IO) {
        try { commitmentsState.value.find { it.name == name }?.let { repository.softDeleteCommitmentToTrash(it) }; repository.deleteCommitment(name) } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed)) }
    }

    fun saveCustomCategory(name: String, tabType: String, emoji: String) = viewModelScope.launch(Dispatchers.IO) {
        try { repository.saveCustomCategory(CustomCategory(name = name, tabType = tabType, iconEmoji = emoji)) } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_save_failed)) }
    }

    fun deleteCustomCategory(customCategory: CustomCategory) = viewModelScope.launch(Dispatchers.IO) {
        try { repository.deleteCustomCategory(customCategory) } catch (e: Exception) { e.printStackTrace(); sendUiEvent(UiEvent.ShowToast(R.string.toast_delete_failed)) }
    }

    fun deleteAllData() = viewModelScope.launch(Dispatchers.IO) { repository.deleteAllData(); refreshLocalBackups() }
    fun clearLocalCopyAndWipeMemory(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllData()
            try {
                context.cacheDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            refreshLocalBackups()
        }
    }

    fun getBaseBackupDirectory(): File = backupService.getBaseBackupDirectory()
    fun getBackupDirectory(): File = backupService.getBackupDirectory()

    fun refreshLocalBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = backupService.scanLocalBackups()
            _uiState.update { it.copy(localBackups = files) }
        }
    }

    fun handleGoogleOAuthCode(code: String, email: String? = null, redirectUri: String = "", onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = googleDriveSyncHelper.handleAuthorizationCode(code, email, redirectUri)
            if (success) repository.saveSettings(settingsState.value.copy(isCloudSyncEnabled = true))
            onComplete?.invoke(success)
        }
    }

    fun backupToGoogleDriveDirect(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = backupService.backupToGoogleDriveDirect(
                googleDriveSyncHelper, settingsState.value, commitmentsState.value, transactionsState.value, deletedItemsFlow.first()
            )
            launch(Dispatchers.Main) {
                if (!success) android.widget.Toast.makeText(getApplication(), R.string.toast_backup_export_failed, android.widget.Toast.LENGTH_LONG).show()
                onComplete?.invoke(success)
            }
        }
    }

    fun restoreFromGoogleDriveDirect(context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupService.restoreFromGoogleDriveDirect(googleDriveSyncHelper, context)
            val success = result.first
            launch(Dispatchers.Main) {
                if (success) {
                    refreshLocalBackups()
                    android.widget.Toast.makeText(context, R.string.cloud_toast_restore_success, android.widget.Toast.LENGTH_SHORT).show()
                    onComplete(true)
                } else {
                    android.widget.Toast.makeText(context, R.string.cloud_toast_restore_failed, android.widget.Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }
        }
    }

    fun googleDriveLogout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch { repository.saveSettings(settingsState.value.copy(isCloudSyncEnabled = false)) }
        googleDriveSyncHelper.logoutAsync {
            _uiState.update { it.copy(cloudBackupsList = emptyList()) }
            onComplete?.invoke()
        }
    }

    fun fetchCloudBackupsList() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isFetchingCloudBackups = true) }
                val list = googleDriveSyncHelper.listCloudBackups()
                _uiState.update { it.copy(cloudBackupsList = list, isFetchingCloudBackups = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isFetchingCloudBackups = false) }
            }
        }
    }

    fun uploadBackupToGoogleDrive(onComplete: (Boolean) -> Unit) {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date())
        uploadBackupToGoogleDriveWithFilename("Mzd_$dateStr.mzd", onComplete)
    }

    fun uploadBackupToGoogleDriveWithFilename(filename: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = backupService.uploadBackupToGoogleDriveWithFilename(
                googleDriveSyncHelper, filename, settingsState.value, commitmentsState.value, transactionsState.value, deletedItemsFlow.first()
            )
            if (success) fetchCloudBackupsList()
            launch(Dispatchers.Main) {
                if (!success) android.widget.Toast.makeText(getApplication(), R.string.toast_backup_export_failed, android.widget.Toast.LENGTH_LONG).show()
                onComplete(success)
            }
        }
    }

    fun restoreFromGoogleDriveById(context: Context, fileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupService.restoreFromGoogleDriveById(googleDriveSyncHelper, context, fileId)
            val success = result.first
            launch(Dispatchers.Main) {
                if (success) {
                    refreshLocalBackups()
                    android.widget.Toast.makeText(context, R.string.cloud_toast_restore_success, android.widget.Toast.LENGTH_SHORT).show()
                    onComplete(true)
                } else {
                    android.widget.Toast.makeText(context, R.string.cloud_toast_restore_failed, android.widget.Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }
        }
    }

    fun deleteCloudBackupById(fileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = backupService.deleteCloudBackupById(googleDriveSyncHelper, fileId)
            if (success) fetchCloudBackupsList()
            launch(Dispatchers.Main) { onComplete(success) }
        }
    }

    fun deleteMultipleCloudBackupsByIds(fileIds: List<String>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var allSuccess = true
            for (fileId in fileIds) {
                if (!backupService.deleteCloudBackupById(googleDriveSyncHelper, fileId)) allSuccess = false
            }
            if (fileIds.isNotEmpty()) fetchCloudBackupsList()
            launch(Dispatchers.Main) { onComplete(allSuccess) }
        }
    }

    fun getBackupJsonForClipboard(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonStr = backupService.getBackupJsonForClipboard(
                settingsState.value, commitmentsState.value, transactionsState.value, deletedItemsFlow.first()
            )
            launch(Dispatchers.Main) { onComplete(jsonStr) }
        }
    }

    fun createLocalBackup(context: Context, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = backupService.createLocalBackup(
                settingsState.value, commitmentsState.value, transactionsState.value,
                repository.getAllCustomersDirect(), repository.getAllTransactionsDirect(), deletedItemsFlow.first(), isSilent = false
            )
            launch(Dispatchers.Main) {
                if (file != null) {
                    refreshLocalBackups()
                    android.widget.Toast.makeText(context, R.string.autobackup_notification_title_local, android.widget.Toast.LENGTH_SHORT).show()
                    onComplete(file)
                } else {
                    android.widget.Toast.makeText(context, R.string.autobackup_notification_title_failure, android.widget.Toast.LENGTH_LONG).show()
                    onComplete(null)
                }
            }
        }
    }

    fun triggerSilentLocalBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            backupService.triggerSilentLocalBackup(
                viewModelScope, settingsState.value, commitmentsState.value, transactionsState.value, deletedItemsFlow.first()
            ) {
                refreshLocalBackups()
            }
        }
    }

    fun executeMasterRestore(rawJsonString: String, context: Context, onComplete: (Boolean, AppSettings?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupService.executeMasterRestore(rawJsonString)
            val success = result.first
            val restoredSettings = result.second
            launch(Dispatchers.Main) {
                if (success) {
                    refreshLocalBackups()
                    val hasLegacy = rawJsonString.contains("mizan_al_dar_db") || rawJsonString.contains("habayeb_debts_db")
                    val successMessageRes = if (hasLegacy) R.string.toast_restore_legacy_migrated else R.string.cloud_toast_restore_success
                    android.widget.Toast.makeText(context, successMessageRes, android.widget.Toast.LENGTH_SHORT).show()
                    onComplete(true, restoredSettings)
                } else {
                    val isSchemaMismatch = rawJsonString.contains("JSONException")
                    val errMsg = if (isSchemaMismatch) R.string.backup_schema_mismatch else R.string.cloud_toast_restore_failed
                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                    onComplete(false, null)
                }
            }
        }
    }

    fun restoreFromMzdContent(jsonContent: String, context: Context, onComplete: (Boolean) -> Unit) {
        executeMasterRestore(jsonContent, context) { success, _ -> onComplete(success) }
    }

    fun restoreFromLocalFile(file: File, context: Context, onComplete: (Boolean, AppSettings?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    executeMasterRestore(file.readText(), context) { success, restoredSettings -> onComplete(success, restoredSettings) }
                } else {
                    launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, R.string.cloud_toast_restore_failed, android.widget.Toast.LENGTH_SHORT).show()
                        onComplete(false, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) { onComplete(false, null) }
            }
        }
    }

    fun formatCurrency(amount: BigDecimal, symbol: String = ""): String = FinanceFormatter.formatCurrency(getApplication(), amount, symbol)
    fun formatDoubleCurrency(amount: Double, symbol: String = ""): String = FinanceFormatter.formatDoubleCurrency(getApplication(), amount, symbol)

    override fun onCleared() {
        super.onCleared()
        appPreferencesRepository.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }
}

typealias MonthLedger = com.example.domain.usecase.MonthLedger
typealias DayLedger = com.example.domain.usecase.DayLedger
