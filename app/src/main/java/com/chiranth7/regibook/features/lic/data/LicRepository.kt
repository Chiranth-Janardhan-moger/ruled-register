package com.chiranth7.regibook.features.lic.data

import kotlinx.coroutines.flow.Flow

class LicRepository(private val dao: LicDao) {
    val allAccounts: Flow<List<LicAccount>> = dao.getAllAccounts()

    fun getAccountById(id: Long): Flow<LicAccount?> = dao.getAccountById(id)

    fun searchAccounts(query: String): Flow<List<LicAccount>> = dao.searchAccounts(query)

    suspend fun insert(account: LicAccount): Long = dao.insertAccount(account)

    suspend fun update(account: LicAccount) = dao.updateAccount(account)

    suspend fun delete(account: LicAccount) = dao.deleteAccount(account)

    suspend fun getAccountByPolicyNumber(policyNumber: String): LicAccount? =
        dao.getAccountByPolicyNumber(policyNumber)

    suspend fun getCount(): Int = dao.getAccountCount()
}
