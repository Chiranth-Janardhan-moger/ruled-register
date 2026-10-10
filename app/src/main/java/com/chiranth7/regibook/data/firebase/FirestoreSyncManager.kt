package com.chiranth7.regibook.data.firebase

import android.content.Context
import com.chiranth7.regibook.data.AppDatabase
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.pigmi.data.PigmiAccount
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirestoreSyncManager {

    private const val COLLECTION_POLICIES = "lic_policies"
    private const val COLLECTION_PIGMI = "pigmi_accounts"

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Seeds initial local policies into Firestore if Firestore is empty.
     */
    suspend fun initialSeedIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val licDao = database.licDao()

            val snapshot = firestore.collection(COLLECTION_POLICIES).limit(1).get().await()
            if (snapshot.isEmpty) {
                val localPolicies = licDao.getAllAccountsList()
                for (account in localPolicies) {
                    if (account.policyNumber.isNotBlank()) {
                        savePolicyToCloud(account)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Saves or updates a single LIC policy in Firestore.
     */
    suspend fun savePolicyToCloud(account: LicAccount, context: Context? = null) = withContext(Dispatchers.IO) {
        if (account.policyNumber.isBlank()) return@withContext
        try {
            val docRef = firestore.collection(COLLECTION_POLICIES).document(account.policyNumber.trim())
            val data = mapOf(
                "name" to account.name,
                "kannadaName" to account.kannadaName,
                "policyNumber" to account.policyNumber,
                "policyName" to account.policyName,
                "totalYears" to account.totalYears,
                "phoneNumber" to account.phoneNumber,
                "lastPaymentDate" to account.lastPaymentDate,
                "nextPaymentDate" to account.nextPaymentDate,
                "premiumAmount" to account.premiumAmount,
                "sumAssured" to account.address, // address holds sumAssured
                "commencementDate" to account.commencementDate,
                "lastPremiumDate" to account.lastPremiumDate,
                "maturityDate" to account.maturityDate,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (_: Exception) {}
    }

    /**
     * Deletes a policy from Firestore.
     */
    suspend fun deletePolicyFromCloud(policyNumber: String) = withContext(Dispatchers.IO) {
        if (policyNumber.isBlank()) return@withContext
        try {
            firestore.collection(COLLECTION_POLICIES).document(policyNumber.trim()).delete().await()
        } catch (_: Exception) {}
    }

    /**
     * Pulls latest policies from Firestore into the local Room database.
     */
    suspend fun syncPoliciesFromCloud(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val licDao = database.licDao()

            val snapshot = firestore.collection(COLLECTION_POLICIES).get().await()
            var count = 0

            for (doc in snapshot.documents) {
                val policyNumber = doc.getString("policyNumber") ?: doc.id
                if (policyNumber.isBlank()) continue

                val existing = licDao.getAccountByPolicyNumber(policyNumber)
                val account = LicAccount(
                    id = existing?.id ?: 0,
                    name = doc.getString("name") ?: existing?.name ?: "",
                    kannadaName = doc.getString("kannadaName") ?: existing?.kannadaName ?: "",
                    policyNumber = policyNumber,
                    policyName = doc.getString("policyName") ?: existing?.policyName ?: "",
                    totalYears = doc.getString("totalYears") ?: existing?.totalYears ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: existing?.phoneNumber ?: "",
                    lastPaymentDate = doc.getString("lastPaymentDate") ?: existing?.lastPaymentDate ?: "",
                    nextPaymentDate = doc.getString("nextPaymentDate") ?: existing?.nextPaymentDate ?: "",
                    premiumAmount = doc.getString("premiumAmount") ?: existing?.premiumAmount ?: "",
                    address = doc.getString("sumAssured") ?: existing?.address ?: "",
                    commencementDate = doc.getString("commencementDate") ?: existing?.commencementDate ?: "",
                    lastPremiumDate = doc.getString("lastPremiumDate") ?: existing?.lastPremiumDate ?: "",
                    maturityDate = doc.getString("maturityDate") ?: existing?.maturityDate ?: ""
                )

                if (existing != null) {
                    licDao.updateAccount(account)
                } else {
                    licDao.insertAccount(account)
                }
                count++
            }
            count
        } catch (e: Exception) {
            com.chiranth7.regibook.util.log.AppLogManager.log(
                context,
                "FirestoreSync",
                "Cloud fetch failed: ${e.message}",
                isError = true
            )
            0
        }
    }

    /**
     * Saves or updates a Pigmi account in Firestore.
     */
    suspend fun savePigmiToCloud(account: PigmiAccount) = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(COLLECTION_PIGMI).document(account.srNo.toString())
            val data = mapOf(
                "srNo" to account.srNo,
                "name" to account.name,
                "kannadaName" to account.kannadaName,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (_: Exception) {}
    }

    /**
     * Pulls latest Pigmi accounts from Firestore into local Room database.
     */
    suspend fun syncPigmiFromCloud(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val pigmiDao = database.pigmiDao()

            val snapshot = firestore.collection(COLLECTION_PIGMI).get().await()
            var count = 0

            for (doc in snapshot.documents) {
                val srNo = doc.getLong("srNo")?.toInt() ?: doc.id.toIntOrNull() ?: continue
                val existing = pigmiDao.getAccountBySrNo(srNo)
                val account = PigmiAccount(
                    id = existing?.id ?: 0,
                    srNo = srNo,
                    name = doc.getString("name") ?: existing?.name ?: "",
                    kannadaName = doc.getString("kannadaName") ?: existing?.kannadaName ?: ""
                )
                if (existing != null) {
                    pigmiDao.updateAccount(account)
                } else {
                    pigmiDao.insertAccount(account)
                }
                count++
            }
            count
        } catch (_: Exception) {
            0
        }
    }
}
