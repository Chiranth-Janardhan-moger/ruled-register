package com.chiranth7.regibook.features.pigmi.data

import kotlinx.coroutines.flow.Flow

class PigmiRepository(private val dao: PigmiDao) {
    val allAccounts: Flow<List<PigmiAccount>> = dao.getAllAccounts()

    fun getAccountById(id: Long): Flow<PigmiAccount?> = dao.getAccountById(id)

    fun searchAccounts(query: String): Flow<List<PigmiAccount>> = dao.searchAccounts(query)

    suspend fun insert(account: PigmiAccount): Long = dao.insertAccount(account)

    suspend fun update(account: PigmiAccount) = dao.updateAccount(account)

    suspend fun delete(account: PigmiAccount) = dao.deleteAccount(account)

    suspend fun getNextSrNo(): Int {
        val max = dao.getMaxSrNo() ?: 0
        return max + 1
    }
}
