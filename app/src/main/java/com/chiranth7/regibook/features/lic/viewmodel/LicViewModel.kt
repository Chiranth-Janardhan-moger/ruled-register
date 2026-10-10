package com.chiranth7.regibook.features.lic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.data.LicRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.features.lic.util.PaymentReminderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LicFormState(
    val editingId: Long? = null,
    val name: String = "",
    val policyNumber: String = "",
    val policyName: String = "",
    val totalYears: String = "",
    val lastPaymentDate: String = "",
    val nextPaymentDate: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val premiumAmount: String = "",
    val nameError: String? = null,
    val policyNumberError: String? = null,
    val isSaving: Boolean = false
)

class LicViewModel(
    private val repository: LicRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun syncPolicies(
        context: android.content.Context,
        settingsManager: com.chiranth7.regibook.util.SettingsManager,
        customUrl: String? = null,
        isSilent: Boolean = false
    ) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            // First ensure local and Firestore have initial seed if empty
            com.chiranth7.regibook.data.firebase.FirestoreSyncManager.initialSeedIfEmpty(context)

            // Sync from Firestore first
            val firestoreCount = try {
                com.chiranth7.regibook.data.firebase.FirestoreSyncManager.syncPoliciesFromCloud(context)
            } catch (_: Exception) { 0 }

            // Then sync from legacy URL / GitHub fallback
            val result = com.chiranth7.regibook.features.lic.sync.LicSyncManager.syncPolicies(
                context = context,
                settingsManager = settingsManager,
                customUrl = customUrl
            )
            _isSyncing.value = false
            if (!isSilent) {
                if (firestoreCount > 0) {
                    _syncMessage.value = context.getString(
                        com.chiranth7.regibook.R.string.sync_success,
                        firestoreCount
                    )
                } else {
                    when (result) {
                        is com.chiranth7.regibook.features.lic.sync.LicSyncManager.SyncResult.Success -> {
                            _syncMessage.value = context.getString(
                                com.chiranth7.regibook.R.string.sync_success,
                                result.count
                            )
                        }
                        is com.chiranth7.regibook.features.lic.sync.LicSyncManager.SyncResult.Error -> {
                            _syncMessage.value = context.getString(com.chiranth7.regibook.R.string.sync_failed)
                        }
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            // Remove old mock sample policies from earlier versions if present
            val oldMockPolicies = listOf("POL-8923410", "POL-4521908", "POL-6638192")
            for (oldPolicy in oldMockPolicies) {
                repository.getAccountByPolicyNumber(oldPolicy)?.let {
                    repository.delete(it)
                }
            }

            // Ensure Chiranth Janardhan Moger policy exists with Sum Assured and key dates
            val existing = repository.getAccountByPolicyNumber("739558210")
            if (existing == null) {
                repository.insert(
                    LicAccount(
                        name = "Chiranth Janardhan Moger",
                        policyNumber = "739558210",
                        policyName = "736 - JEEVAN LABH PLAN",
                        totalYears = "21/15",
                        lastPaymentDate = "29/06/2025",
                        nextPaymentDate = "28/06/2026",
                        premiumAmount = "₹11,873/Year",
                        address = "₹2,00,000",
                        phoneNumber = "9071911793",
                        commencementDate = "28/06/2025",
                        lastPremiumDate = "28/06/2040",
                        maturityDate = "28/06/2046",
                        kannadaName = "ಚಿರಂತ ಜನಾರ್ದನ ಮೊಗೇರ"
                    )
                )
            } else if (existing.commencementDate.isBlank() || existing.lastPremiumDate.isBlank() || existing.maturityDate.isBlank() || existing.address != "₹2,00,000" || existing.kannadaName.isBlank() || existing.phoneNumber.isBlank()) {
                repository.update(
                    existing.copy(
                        address = "₹2,00,000",
                        phoneNumber = if (existing.phoneNumber.isBlank()) "9071911793" else existing.phoneNumber,
                        commencementDate = "28/06/2025",
                        lastPremiumDate = "28/06/2040",
                        maturityDate = "28/06/2046",
                        kannadaName = "ಚಿರಂತ ಜನಾರ್ದನ ಮೊಗೇರ"
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts: StateFlow<List<LicAccount>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allAccounts
            } else {
                repository.searchAccounts(query.trim())
            }
        }
        .map { list -> sortAccountsByUrgency(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun getUrgencyRank(status: PaymentReminderStatus): Int = when (status) {
        PaymentReminderStatus.OVERDUE -> 0    // Red: Overdue comes first
        PaymentReminderStatus.DUE_TODAY -> 1  // Due today
        PaymentReminderStatus.DUE_SOON -> 2   // Amber: Due in next few days
        PaymentReminderStatus.UPCOMING -> 3   // Green: Normal upcoming
        PaymentReminderStatus.NOT_SET -> 4
        PaymentReminderStatus.COMPLETED -> 5  // Completed / Matured
    }

    fun sortAccountsByUrgency(accounts: List<LicAccount>): List<LicAccount> {
        return accounts.sortedWith(
            compareBy<LicAccount> { account ->
                val reminder = PaymentReminderHelper.calculateReminder(account.nextPaymentDate)
                getUrgencyRank(reminder.status)
            }.thenBy { account ->
                PaymentReminderHelper.parseDate(account.nextPaymentDate)?.time ?: Long.MAX_VALUE
            }
        )
    }

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedAccount: StateFlow<LicAccount?> = _selectedAccountId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getAccountById(id)
            } else {
                MutableStateFlow(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _formState = MutableStateFlow(LicFormState())
    val formState: StateFlow<LicFormState> = _formState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAccount(id: Long?) {
        _selectedAccountId.value = id
    }

    fun prepareNewAccount() {
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        // Default next payment date to next month or next year
        val nextCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
        }
        val defaultNextDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(nextCal.time)

        _formState.value = LicFormState(
            editingId = null,
            lastPaymentDate = todayStr,
            nextPaymentDate = defaultNextDate,
            totalYears = "15 Years"
        )
    }

    fun prepareEditAccount(account: LicAccount) {
        _formState.value = LicFormState(
            editingId = account.id,
            name = account.name,
            policyNumber = account.policyNumber,
            policyName = account.policyName,
            totalYears = account.totalYears,
            lastPaymentDate = account.lastPaymentDate,
            nextPaymentDate = account.nextPaymentDate,
            phoneNumber = account.phoneNumber,
            address = account.address,
            premiumAmount = account.premiumAmount
        )
    }

    fun onNameChanged(value: String) {
        _formState.value = _formState.value.copy(
            name = value,
            nameError = null
        )
    }

    fun onPolicyNumberChanged(value: String) {
        _formState.value = _formState.value.copy(
            policyNumber = value,
            policyNumberError = null
        )
    }

    fun onPolicyNameChanged(value: String) {
        _formState.value = _formState.value.copy(policyName = value)
    }

    fun onTotalYearsChanged(value: String) {
        _formState.value = _formState.value.copy(totalYears = value)
    }

    fun onLastPaymentDateChanged(value: String) {
        _formState.value = _formState.value.copy(lastPaymentDate = value)
    }

    fun onNextPaymentDateChanged(value: String) {
        _formState.value = _formState.value.copy(nextPaymentDate = value)
    }

    fun onPhoneNumberChanged(value: String) {
        _formState.value = _formState.value.copy(phoneNumber = value)
    }

    fun onAddressChanged(value: String) {
        _formState.value = _formState.value.copy(address = value)
    }

    fun onPremiumAmountChanged(value: String) {
        _formState.value = _formState.value.copy(premiumAmount = value)
    }

    fun saveAccount(
        nameRequiredError: String,
        policyRequiredError: String,
        onSuccess: (Long) -> Unit
    ) {
        val current = _formState.value

        var hasError = false
        var nameErr: String? = null
        var policyErr: String? = null

        if (current.name.trim().isBlank()) {
            nameErr = nameRequiredError
            hasError = true
        }

        if (current.policyNumber.trim().isBlank()) {
            policyErr = policyRequiredError
            hasError = true
        }

        if (hasError) {
            _formState.value = current.copy(
                nameError = nameErr,
                policyNumberError = policyErr
            )
            return
        }

        val name = current.name.trim()
        val policyNo = current.policyNumber.trim()
        val policyName = current.policyName.trim()
        val totalYears = current.totalYears.trim()
        val lastDate = current.lastPaymentDate.trim()
        val nextDate = current.nextPaymentDate.trim()
        val phone = current.phoneNumber.trim()
        val address = current.address.trim()
        val premium = current.premiumAmount.trim()

        viewModelScope.launch {
            _formState.value = current.copy(isSaving = true)
            val editingId = current.editingId
            val finalId: Long
            if (editingId != null) {
                val updated = LicAccount(
                    id = editingId,
                    name = name,
                    policyNumber = policyNo,
                    policyName = policyName,
                    totalYears = totalYears,
                    lastPaymentDate = lastDate,
                    nextPaymentDate = nextDate,
                    phoneNumber = phone,
                    address = address,
                    premiumAmount = premium
                )
                repository.update(updated)
                com.chiranth7.regibook.data.firebase.FirestoreSyncManager.savePolicyToCloud(updated)
                finalId = editingId
            } else {
                val newAccount = LicAccount(
                    name = name,
                    policyNumber = policyNo,
                    policyName = policyName,
                    totalYears = totalYears,
                    lastPaymentDate = lastDate,
                    nextPaymentDate = nextDate,
                    phoneNumber = phone,
                    address = address,
                    premiumAmount = premium
                )
                finalId = repository.insert(newAccount)
                com.chiranth7.regibook.data.firebase.FirestoreSyncManager.savePolicyToCloud(newAccount.copy(id = finalId))
            }
            _formState.value = LicFormState()
            _selectedAccountId.value = finalId
            onSuccess(finalId)
        }
    }

    fun deleteAccount(account: LicAccount, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.delete(account)
            com.chiranth7.regibook.data.firebase.FirestoreSyncManager.deletePolicyFromCloud(account.policyNumber)
            if (_selectedAccountId.value == account.id) {
                _selectedAccountId.value = null
            }
            onSuccess()
        }
    }

    fun markPolicyAsPaid(
        account: LicAccount,
        newNextPaymentDate: String,
        newLastPaymentDate: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val updated = account.copy(
                nextPaymentDate = newNextPaymentDate,
                lastPaymentDate = newLastPaymentDate
            )
            repository.update(updated)
            com.chiranth7.regibook.data.firebase.FirestoreSyncManager.savePolicyToCloud(updated)
            onSuccess?.invoke()
        }
    }
}

class LicViewModelFactory(
    private val repository: LicRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LicViewModel::class.java)) {
            return LicViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
