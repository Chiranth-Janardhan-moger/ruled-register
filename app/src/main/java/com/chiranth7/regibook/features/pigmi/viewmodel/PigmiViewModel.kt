package com.chiranth7.regibook.features.pigmi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chiranth7.regibook.features.pigmi.data.PigmiAccount
import com.chiranth7.regibook.features.pigmi.data.PigmiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PigmiFormState(
    val editingId: Long? = null,
    val srNo: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val accountNumber: String = "",
    val dailyAmount: String = "",
    val srNoError: String? = null,
    val nameError: String? = null,
    val isSaving: Boolean = false
)

class PigmiViewModel(
    private val repository: PigmiRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts: StateFlow<List<PigmiAccount>> = _searchQuery
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
    val selectedAccount: StateFlow<PigmiAccount?> = _selectedAccountId
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

    private val _formState = MutableStateFlow(PigmiFormState())
    val formState: StateFlow<PigmiFormState> = _formState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAccount(id: Long?) {
        _selectedAccountId.value = id
    }

    fun prepareNewAccount() {
        viewModelScope.launch {
            val nextSr = repository.getNextSrNo()
            _formState.value = PigmiFormState(
                editingId = null,
                srNo = nextSr.toString()
            )
        }
    }

    fun prepareEditAccount(account: PigmiAccount) {
        _formState.value = PigmiFormState(
            editingId = account.id,
            srNo = account.srNo.toString(),
            name = account.name,
            phoneNumber = account.phoneNumber,
            address = account.address,
            accountNumber = account.accountNumber,
            dailyAmount = account.dailyAmount
        )
    }

    fun onSrNoChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _formState.value = _formState.value.copy(
            srNo = filtered,
            srNoError = null
        )
    }

    fun onNameChanged(value: String) {
        _formState.value = _formState.value.copy(
            name = value,
            nameError = null
        )
    }

    fun onPhoneNumberChanged(value: String) {
        _formState.value = _formState.value.copy(phoneNumber = value)
    }

    fun onAddressChanged(value: String) {
        _formState.value = _formState.value.copy(address = value)
    }

    fun onAccountNumberChanged(value: String) {
        _formState.value = _formState.value.copy(accountNumber = value)
    }

    fun onDailyAmountChanged(value: String) {
        _formState.value = _formState.value.copy(dailyAmount = value)
    }

    fun saveAccount(
        srNoRequiredError: String,
        nameRequiredError: String,
        onSuccess: (Long) -> Unit
    ) {
        val current = _formState.value
        val parsedSrNo = current.srNo.toIntOrNull()

        var hasError = false
        var srErr: String? = null
        var nameErr: String? = null

        if (parsedSrNo == null || parsedSrNo <= 0) {
            srErr = srNoRequiredError
            hasError = true
        }

        if (current.name.trim().isBlank()) {
            nameErr = nameRequiredError
            hasError = true
        }

        if (hasError) {
            _formState.value = current.copy(
                srNoError = srErr,
                nameError = nameErr
            )
            return
        }

        val srNo = parsedSrNo ?: 1
        val name = current.name.trim()
        val phone = current.phoneNumber.trim()
        val address = current.address.trim()
        val accNo = current.accountNumber.trim()
        val daily = current.dailyAmount.trim()

        viewModelScope.launch {
            _formState.value = current.copy(isSaving = true)
            val editingId = current.editingId
            val finalId: Long
            if (editingId != null) {
                val updated = PigmiAccount(
                    id = editingId,
                    srNo = srNo,
                    name = name,
                    phoneNumber = phone,
                    address = address,
                    accountNumber = accNo,
                    dailyAmount = daily
                )
                repository.update(updated)
                finalId = editingId
            } else {
                val newAccount = PigmiAccount(
                    srNo = srNo,
                    name = name,
                    phoneNumber = phone,
                    address = address,
                    accountNumber = accNo,
                    dailyAmount = daily
                )
                finalId = repository.insert(newAccount)
            }
            _formState.value = PigmiFormState()
            _selectedAccountId.value = finalId
            onSuccess(finalId)
        }
    }

    fun deleteAccount(account: PigmiAccount, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.delete(account)
            if (_selectedAccountId.value == account.id) {
                _selectedAccountId.value = null
            }
            onSuccess()
        }
    }
}

class PigmiViewModelFactory(
    private val repository: PigmiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PigmiViewModel::class.java)) {
            return PigmiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
