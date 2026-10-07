package com.chiranth7.regibook.features.lic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LicDao {
    @Query("SELECT * FROM lic_accounts ORDER BY id DESC")
    fun getAllAccounts(): Flow<List<LicAccount>>

    @Query("SELECT * FROM lic_accounts ORDER BY id DESC")
    suspend fun getAllAccountsList(): List<LicAccount>

    @Query("SELECT * FROM lic_accounts WHERE id = :id LIMIT 1")
    fun getAccountById(id: Long): Flow<LicAccount?>

    @Query("SELECT * FROM lic_accounts WHERE name LIKE '%' || :query || '%' OR policyNumber LIKE '%' || :query || '%' OR policyName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchAccounts(query: String): Flow<List<LicAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: LicAccount): Long

    @Update
    suspend fun updateAccount(account: LicAccount)

    @Delete
    suspend fun deleteAccount(account: LicAccount)

    @Query("SELECT * FROM lic_accounts WHERE policyNumber = :policyNumber LIMIT 1")
    suspend fun getAccountByPolicyNumber(policyNumber: String): LicAccount?

    @Query("SELECT COUNT(*) FROM lic_accounts")
    suspend fun getAccountCount(): Int
}
