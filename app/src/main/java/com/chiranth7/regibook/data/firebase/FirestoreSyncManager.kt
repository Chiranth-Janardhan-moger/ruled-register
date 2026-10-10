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
     * Seeds initial local policies and pigmi accounts from bundled assets into Room
     * and uploads them to Firestore if Firestore is empty.
     */
    suspend fun initialSeedIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val licDao = database.licDao()
        val pigmiDao = database.pigmiDao()

        // 1. Seed LIC policies into Room if Room has 0 or only 1 policy
        try {
            if (licDao.getAccountCount() <= 1) {
                val assetJson = context.assets.open("policies-sync.json").bufferedReader().use { it.readText() }
                val accounts = parsePoliciesJson(assetJson)
                for (acc in accounts) {
                    if (acc.policyNumber.isNotBlank()) {
                        val existing = licDao.getAccountByPolicyNumber(acc.policyNumber)
                        if (existing == null) {
                            licDao.insertAccount(acc)
                        } else {
                            licDao.updateAccount(existing.copy(
                                name = acc.name.ifBlank { existing.name },
                                kannadaName = acc.kannadaName.ifBlank { existing.kannadaName },
                                policyName = acc.policyName.ifBlank { existing.policyName },
                                totalYears = acc.totalYears.ifBlank { existing.totalYears },
                                phoneNumber = acc.phoneNumber.ifBlank { existing.phoneNumber },
                                lastPaymentDate = acc.lastPaymentDate.ifBlank { existing.lastPaymentDate },
                                nextPaymentDate = acc.nextPaymentDate.ifBlank { existing.nextPaymentDate },
                                premiumAmount = acc.premiumAmount.ifBlank { existing.premiumAmount },
                                address = acc.address.ifBlank { existing.address },
                                commencementDate = acc.commencementDate.ifBlank { existing.commencementDate },
                                lastPremiumDate = acc.lastPremiumDate.ifBlank { existing.lastPremiumDate },
                                maturityDate = acc.maturityDate.ifBlank { existing.maturityDate }
                            ))
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Seed Pigmi accounts into Room if empty
        try {
            if (pigmiDao.getAllAccountsSnapshot().isEmpty()) {
                val assetJson = context.assets.open("pigmi-sync.json").bufferedReader().use { it.readText() }
                val pigmiAccounts = parsePigmiJson(assetJson)
                for (acc in pigmiAccounts) {
                    if (acc.srNo > 0) {
                        val existing = pigmiDao.getAccountBySrNo(acc.srNo)
                        if (existing == null) {
                            pigmiDao.insertAccount(acc)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Upload to Firestore if Firestore collections are empty
        try {
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

        try {
            val pigmiSnapshot = firestore.collection(COLLECTION_PIGMI).limit(1).get().await()
            if (pigmiSnapshot.isEmpty) {
                val localPigmi = pigmiDao.getAllAccountsSnapshot()
                for (account in localPigmi) {
                    savePigmiToCloud(account)
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
     * Deletes a Pigmi account from Firestore.
     */
    suspend fun deletePigmiFromCloud(srNo: Int) = withContext(Dispatchers.IO) {
        if (srNo <= 0) return@withContext
        try {
            firestore.collection(COLLECTION_PIGMI).document(srNo.toString()).delete().await()
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

    private fun parsePoliciesJson(jsonString: String): List<LicAccount> {
        val trimmed = jsonString.trim()
        val jsonArray = when {
            trimmed.startsWith("[") -> org.json.JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = org.json.JSONObject(trimmed)
                when {
                    obj.has("policies") -> obj.getJSONArray("policies")
                    obj.has("accounts") -> obj.getJSONArray("accounts")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> org.json.JSONArray()
                }
            }
            else -> return emptyList()
        }

        val list = mutableListOf<LicAccount>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue

            val policyNumber = item.optString("policyNumber", item.optString("policy_number", "")).trim()
            val name = item.optString("name", item.optString("customerName", item.optString("customer_name", ""))).trim()
            if (policyNumber.isBlank() && name.isBlank()) continue

            val policyName = item.optString("policyName", item.optString("policy_name", item.optString("plan", ""))).trim()
            val totalYears = item.optString("totalYears", item.optString("total_years", item.optString("termPpt", item.optString("term_ppt", "")))).trim()
            val phoneNumber = item.optString("phoneNumber", item.optString("phone_number", item.optString("phone", ""))).trim()
            val lastPaymentDate = item.optString("lastPaymentDate", item.optString("last_payment_date", item.optString("last_paid", ""))).trim()
            val nextPaymentDate = item.optString("nextPaymentDate", item.optString("next_payment_date", item.optString("next_due_date", item.optString("fup", "")))).trim()
            val premiumAmount = item.optString("premiumAmount", item.optString("premium_amount", item.optString("premium", ""))).trim()
            val sumAssured = item.optString("sumAssured", item.optString("sum_assured", item.optString("address", ""))).trim()
            val commencementDate = item.optString("commencementDate", item.optString("commencement_date", "")).trim()
            val lastPremiumDate = item.optString("lastPremiumDate", item.optString("last_premium_date", item.optString("endOfPpt", item.optString("end_of_ppt", "")))).trim()
            val maturityDate = item.optString("maturityDate", item.optString("maturity_date", "")).trim()
            val kannadaName = item.optString("kannadaName", item.optString("kannada_name", item.optString("nameKn", ""))).trim()

            list.add(
                LicAccount(
                    id = 0L,
                    name = name,
                    policyNumber = policyNumber,
                    policyName = policyName,
                    totalYears = totalYears,
                    phoneNumber = phoneNumber,
                    lastPaymentDate = lastPaymentDate,
                    nextPaymentDate = nextPaymentDate,
                    premiumAmount = premiumAmount,
                    address = sumAssured,
                    commencementDate = commencementDate,
                    lastPremiumDate = lastPremiumDate,
                    maturityDate = maturityDate,
                    kannadaName = kannadaName
                )
            )
        }
        return list
    }

    private fun parsePigmiJson(jsonString: String): List<PigmiAccount> {
        val trimmed = jsonString.trim()
        val jsonArray = when {
            trimmed.startsWith("[") -> org.json.JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = org.json.JSONObject(trimmed)
                when {
                    obj.has("pigmi") -> obj.getJSONArray("pigmi")
                    obj.has("accounts") -> obj.getJSONArray("accounts")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> org.json.JSONArray()
                }
            }
            else -> return emptyList()
        }

        val list = mutableListOf<PigmiAccount>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue

            val srNo = item.optInt("srNo", item.optInt("sr_no", item.optInt("id", 0)))
            val name = item.optString("name", item.optString("customerName", item.optString("customer_name", ""))).trim()
            val kannadaName = item.optString("kannadaName", item.optString("kannada_name", item.optString("nameKn", ""))).trim()

            if (srNo <= 0 && name.isBlank()) continue

            list.add(
                PigmiAccount(
                    id = 0L,
                    srNo = srNo,
                    name = name,
                    phoneNumber = "",
                    address = "",
                    accountNumber = "",
                    dailyAmount = "",
                    kannadaName = kannadaName
                )
            )
        }
        return list
    }
}
