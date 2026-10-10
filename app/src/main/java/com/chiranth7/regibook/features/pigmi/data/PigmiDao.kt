package com.chiranth7.regibook.features.pigmi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PigmiDao {
    @Query("SELECT * FROM pigmi_accounts ORDER BY srNo ASC, id ASC")
    fun getAllAccounts(): Flow<List<PigmiAccount>>

    @Query("SELECT * FROM pigmi_accounts WHERE name LIKE '%' || :query || '%' OR kannadaName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR accountNumber LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY srNo ASC")
    fun searchAccounts(query: String): Flow<List<PigmiAccount>>

    @Query("SELECT * FROM pigmi_accounts WHERE srNo = :srNo LIMIT 1")
    suspend fun getAccountBySrNo(srNo: Int): PigmiAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: PigmiAccount): Long

    @Update
    suspend fun updateAccount(account: PigmiAccount)

    @Delete
    suspend fun deleteAccount(account: PigmiAccount)

    @Query("SELECT MAX(srNo) FROM pigmi_accounts")
    suspend fun getMaxSrNo(): Int?
}
