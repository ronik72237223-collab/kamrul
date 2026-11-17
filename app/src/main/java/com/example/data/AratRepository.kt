package com.example.data

import com.example.data.model.AppConfig
import com.example.data.model.AratLedger
import com.example.data.model.CalculatorState
import com.example.data.model.DailyExpense
import com.example.data.model.GradeEntry
import com.example.data.model.SUPER_ADMIN_UID
import com.example.data.model.TrashItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AratRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("ব্যবহারকারী লগইন করা নেই।")
    }

    // 1. Global App Config (Realtime)
    fun observeAppConfig(): Flow<AppConfig> = flow {
        val uid = requireUserId()
        val path = "app_config/global"
        emitAll(
            db.collection("app_config").document("global")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: AppConfig()
                    } else {
                        AppConfig()
                    }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    emit(AppConfig())
                }
        )
    }

    suspend fun updateAppConfig(config: AppConfig): Result<Unit> {
        val path = "app_config/global"
        return try {
            val uid = requireUserId()
            check(uid == SUPER_ADMIN_UID) { "শুধুমাত্র সুপার অ্যাডমিন সেটিংস পরিবর্তন করতে পারবেন।" }
            val cleanGrades = config.qualityGrades
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("SS VIP", "VIP", "B-Grade", "Cat (ক্যাট)") }
                .take(20)
            val payload = mapOf(
                "appName" to config.appName.trim().ifBlank { "স্ট্রবেরি আড়ত প্রো" }.take(60),
                "aratName" to config.aratName.trim().ifBlank { "স্ট্রবেরি সেন্ট্রাল আড়ত" }.take(100),
                "defaultCommissionPercent" to config.defaultCommissionPercent.coerceIn(0.0, 50.0),
                "defaultKuliRate" to config.defaultKuliRate.coerceIn(0.0, 100000.0),
                "welcomeBanner" to config.welcomeBanner.trim().take(300),
                "qualityGrades" to cleanGrades,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_config").document("global").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // 2. User Profile & Admin Users Directory
    suspend fun upsertUserProfile(
        uid: String,
        email: String,
        displayName: String,
        aratName: String
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "email" to email.take(150),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (displayName.isNotBlank()) updates["displayName"] = displayName.trim().take(80)
                if (aratName.isNotBlank()) updates["aratName"] = aratName.trim().take(100)
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "email" to email.take(150),
                    "displayName" to displayName.trim().take(80),
                    "aratName" to aratName.trim().take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        if (uid != SUPER_ADMIN_UID) {
            emit(emptyList())
            return@flow
        }
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // 3. Calculator Active Draft (per UID)
    suspend fun loadTodayDraft(userId: String): CalculatorState? {
        val path = "users/$userId/drafts/today"
        return try {
            val snap = db.collection("users").document(userId)
                .collection("drafts").document("today").get().await()
            if (!snap.exists()) return null
            val rawGrades = (snap.get("grades") as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val weights = (map["cartonWeights"] as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toDouble() }
                    ?: emptyList()
                GradeEntry(
                    gradeId = (map["gradeId"] as? String) ?: "",
                    name = (map["name"] as? String) ?: "",
                    ratePerKg = (map["ratePerKg"] as? Number)?.toDouble() ?: 0.0,
                    cartonWeights = weights
                )
            } ?: emptyList()

            CalculatorState(
                dateText = snap.getString("dateText") ?: "",
                displayDate = snap.getString("displayDate") ?: "",
                aratName = snap.getString("aratName") ?: "",
                supplierName = snap.getString("supplierName") ?: "",
                grades = rawGrades,
                commissionPercent = snap.getDouble("commissionPercent") ?: 7.0,
                kuliExpense = snap.getDouble("kuliExpense") ?: 0.0,
                otherExpense = snap.getDouble("otherExpense") ?: 0.0,
                chalanNote = snap.getString("chalanNote") ?: "",
                paymentMethod = snap.getString("paymentMethod") ?: "নগদ (Cash)",
                signatureText = snap.getString("signatureText") ?: ""
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun saveTodayDraft(userId: String, state: CalculatorState): Result<Unit> {
        val path = "users/$userId/drafts/today"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val gradesPayload = state.grades.take(30).map { g ->
                mapOf(
                    "gradeId" to g.gradeId,
                    "name" to g.name.take(60),
                    "ratePerKg" to g.ratePerKg.coerceAtLeast(0.0),
                    "cartonWeights" to g.cartonWeights
                )
            }
            val payload = mapOf(
                "userId" to uid,
                "dateText" to state.dateText.ifBlank { "2026-11-17" }.take(30),
                "displayDate" to state.displayDate.ifBlank { "আজ" }.take(50),
                "aratName" to state.aratName.take(100),
                "supplierName" to state.supplierName.take(100),
                "grades" to gradesPayload,
                "commissionPercent" to state.commissionPercent.coerceIn(0.0, 100.0),
                "kuliExpense" to state.kuliExpense.coerceIn(0.0, 10000000.0),
                "otherExpense" to state.otherExpense.coerceIn(0.0, 10000000.0),
                "chalanNote" to state.chalanNote.take(500),
                "paymentMethod" to state.paymentMethod.ifBlank { "নগদ (Cash)" }.take(50),
                "signatureText" to state.signatureText.take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("drafts").document("today")
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // 4. Ledgers (History & Soft-Delete Recycle Bin)
    fun observeLedgers(userId: String): Flow<List<AratLedger>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/ledgers"
        emitAll(
            db.collection("users").document(uid).collection("ledgers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AratLedger::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveLedger(userId: String, state: CalculatorState): Result<String> {
        var path = "users/$userId/ledgers"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val col = db.collection("users").document(uid).collection("ledgers")
            val existingId = state.editingLedgerId?.takeIf { it.isNotBlank() }
            val docRef = if (existingId != null) col.document(existingId) else col.document()
            path = docRef.path

            val gradesPayload = state.grades.take(30).map { g ->
                mapOf(
                    "gradeId" to g.gradeId,
                    "name" to g.name.take(60),
                    "ratePerKg" to g.ratePerKg.coerceAtLeast(0.0),
                    "cartonWeights" to g.cartonWeights
                )
            }

            if (existingId != null) {
                val updates = mapOf(
                    "dateText" to state.dateText.ifBlank { "2026-11-17" }.take(30),
                    "displayDate" to state.displayDate.ifBlank { "আজ" }.take(50),
                    "aratName" to state.aratName.take(100),
                    "supplierName" to state.supplierName.take(100),
                    "grades" to gradesPayload,
                    "commissionPercent" to state.commissionPercent.coerceIn(0.0, 100.0),
                    "kuliExpense" to state.kuliExpense.coerceIn(0.0, 10000000.0),
                    "otherExpense" to state.otherExpense.coerceIn(0.0, 10000000.0),
                    "chalanNote" to state.chalanNote.take(500),
                    "paymentMethod" to state.paymentMethod.ifBlank { "নগদ (Cash)" }.take(50),
                    "signatureText" to state.signatureText.take(100),
                    "totalCartons" to state.totalCartons.coerceIn(0L, 100000L),
                    "totalWeightKg" to state.totalWeightKg.coerceIn(0.0, 1000000.0),
                    "grossSales" to state.grossSales.coerceIn(0.0, 100000000.0),
                    "commissionAmount" to state.commissionAmount.coerceIn(0.0, 100000000.0),
                    "netPayable" to state.netPayable.coerceIn(-100000000.0, 100000000.0),
                    "deleted" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "dateText" to state.dateText.ifBlank { "2026-11-17" }.take(30),
                    "displayDate" to state.displayDate.ifBlank { "আজ" }.take(50),
                    "aratName" to state.aratName.take(100),
                    "supplierName" to state.supplierName.take(100),
                    "grades" to gradesPayload,
                    "commissionPercent" to state.commissionPercent.coerceIn(0.0, 100.0),
                    "kuliExpense" to state.kuliExpense.coerceIn(0.0, 10000000.0),
                    "otherExpense" to state.otherExpense.coerceIn(0.0, 10000000.0),
                    "chalanNote" to state.chalanNote.take(500),
                    "paymentMethod" to state.paymentMethod.ifBlank { "নগদ (Cash)" }.take(50),
                    "signatureText" to state.signatureText.take(100),
                    "totalCartons" to state.totalCartons.coerceIn(0L, 100000L),
                    "totalWeightKg" to state.totalWeightKg.coerceIn(0.0, 1000000.0),
                    "grossSales" to state.grossSales.coerceIn(0.0, 100000000.0),
                    "commissionAmount" to state.commissionAmount.coerceIn(0.0, 100000000.0),
                    "netPayable" to state.netPayable.coerceIn(-100000000.0, 100000000.0),
                    "deleted" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setLedgerDeleted(userId: String, ledgerId: String, deleted: Boolean): Result<Unit> {
        val path = "users/$userId/ledgers/$ledgerId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("ledgers").document(ledgerId)
                .update(
                    mapOf(
                        "deleted" to deleted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLedgerPermanently(userId: String, ledgerId: String): Result<Unit> {
        val path = "users/$userId/ledgers/$ledgerId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("ledgers").document(ledgerId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 5. Daily Expenses (হাতখরচ)
    fun observeExpenses(userId: String): Flow<List<DailyExpense>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/expenses"
        emitAll(
            db.collection("users").document(uid).collection("expenses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DailyExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertExpense(
        userId: String,
        expenseId: String?,
        dateText: String,
        category: String,
        description: String,
        amount: Double
    ): Result<Unit> {
        var path = "users/$userId/expenses"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val col = db.collection("users").document(uid).collection("expenses")
            val docRef = if (!expenseId.isNullOrBlank()) col.document(expenseId) else col.document()
            path = docRef.path

            if (!expenseId.isNullOrBlank()) {
                docRef.update(
                    mapOf(
                        "dateText" to dateText.take(30),
                        "category" to category.ifBlank { "অন্যান্য" }.take(60),
                        "description" to description.take(200),
                        "amount" to amount.coerceIn(0.0, 10000000.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateText" to dateText.take(30),
                        "category" to category.ifBlank { "অন্যান্য" }.take(60),
                        "description" to description.take(200),
                        "amount" to amount.coerceIn(0.0, 10000000.0),
                        "deleted" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setExpenseDeleted(userId: String, expenseId: String, deleted: Boolean): Result<Unit> {
        val path = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("expenses").document(expenseId)
                .update(
                    mapOf(
                        "deleted" to deleted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpensePermanently(userId: String, expenseId: String): Result<Unit> {
        val path = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("expenses").document(expenseId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 6. Calculator Trash Items (Deleted Cartons / Grades for 1-Tap Recovery)
    fun observeTrashItems(userId: String): Flow<List<TrashItem>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/trash_items"
        emitAll(
            db.collection("users").document(uid).collection("trash_items")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TrashItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTrashItem(
        userId: String,
        itemType: String,
        gradeName: String,
        weightKg: Double,
        ratePerKg: Double,
        cartonWeights: List<Double>
    ): Result<Unit> {
        val path = "users/$userId/trash_items"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("trash_items").document()
            val payload = mapOf(
                "userId" to uid,
                "itemType" to itemType,
                "gradeName" to gradeName.ifBlank { "অন্যান্য" }.take(60),
                "weightKg" to weightKg.coerceIn(0.0, 10000.0),
                "ratePerKg" to ratePerKg.coerceIn(0.0, 1000000.0),
                "cartonWeights" to cartonWeights.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTrashItemPermanently(userId: String, trashId: String): Result<Unit> {
        val path = "users/$userId/trash_items/$trashId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("trash_items").document(trashId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
