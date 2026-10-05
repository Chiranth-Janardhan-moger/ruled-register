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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts: StateFlow<List<LicAccount>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allAccounts
            } else {
                repository.searchAccounts(query.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
            }
            _formState.value = LicFormState()
            _selectedAccountId.value = finalId
            onSuccess(finalId)
        }
    }

    fun deleteAccount(account: LicAccount, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.delete(account)
            if (_selectedAccountId.value == account.id) {
                _selectedAccountId.value = null
            }
            onSuccess()
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
