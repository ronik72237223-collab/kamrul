package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val SUPER_ADMIN_UID = "cs69FLrgVqMFNyKDqDth9BwZBKt1"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class AppConfig(
    val appName: String = "স্ট্রবেরি আড়ত প্রো",
    val aratName: String = "স্ট্রবেরি সেন্ট্রাল আড়ত",
    val defaultCommissionPercent: Double = 7.0,
    val defaultKuliRate: Double = 0.0,
    val welcomeBanner: String = "আজকের তাজা স্ট্রবেরি কার্টুন হিসাব শুরু করুন",
    val qualityGrades: List<String> = listOf("SS VIP", "VIP", "B-Grade", "Cat (ক্যাট)"),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val aratName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GradeEntry(
    val gradeId: String = "",
    val name: String = "",
    val ratePerKg: Double = 0.0,
    val cartonWeights: List<Double> = emptyList()
) {
    val totalCartons: Int get() = cartonWeights.size
    val totalWeightKg: Double get() = cartonWeights.sum()
    val subtotalSales: Double get() = totalWeightKg * ratePerKg
}

@Keep
@IgnoreExtraProperties
data class CalculatorState(
    val dateText: String = "",
    val displayDate: String = "",
    val aratName: String = "",
    val supplierName: String = "",
    val grades: List<GradeEntry> = emptyList(),
    val commissionPercent: Double = 7.0,
    val kuliExpense: Double = 0.0,
    val otherExpense: Double = 0.0,
    val chalanNote: String = "",
    val paymentMethod: String = "নগদ (Cash)",
    val signatureText: String = "",
    val editingLedgerId: String? = null
) {
    val totalCartons: Long get() = grades.sumOf { it.totalCartons.toLong() }
    val totalWeightKg: Double get() = grades.sumOf { it.totalWeightKg }
    val grossSales: Double get() = grades.sumOf { it.subtotalSales }
    val commissionAmount: Double get() = grossSales * (commissionPercent / 100.0)
    val totalDeductions: Double get() = commissionAmount + kuliExpense + otherExpense
    val netPayable: Double get() = grossSales - totalDeductions
    val weightedAverageRate: Double
        get() = if (totalWeightKg > 0.0) grossSales / totalWeightKg else 0.0

    fun qualitySummaryText(): String =
        grades.filter { it.totalCartons > 0 || it.ratePerKg > 0.0 }
            .joinToString(", ") { "${it.name}: ${it.totalCartons}ব (${String.format("%.1f", it.totalWeightKg)} কেজি)" }
            .ifBlank { "কোনো কার্টুন নেই" }
}

@Keep
@IgnoreExtraProperties
data class AratLedger(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateText: String = "",
    val displayDate: String = "",
    val aratName: String = "",
    val supplierName: String = "",
    val grades: List<GradeEntry> = emptyList(),
    val commissionPercent: Double = 7.0,
    val kuliExpense: Double = 0.0,
    val otherExpense: Double = 0.0,
    val chalanNote: String = "",
    val paymentMethod: String = "নগদ (Cash)",
    val signatureText: String = "",
    val totalCartons: Long = 0L,
    val totalWeightKg: Double = 0.0,
    val grossSales: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val netPayable: Double = 0.0,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalDeductions: Double get() = commissionAmount + kuliExpense + otherExpense
    val averageRate: Double get() = if (totalWeightKg > 0.0) grossSales / totalWeightKg else 0.0

    fun qualityBreakdownText(): String =
        grades.filter { it.totalCartons > 0 }
            .joinToString(" | ") { "${it.name}: ${it.totalCartons} কার্টুন (${String.format("%.1f", it.totalWeightKg)} কেজি @ ৳${it.ratePerKg.toInt()})" }
            .ifBlank { "-" }
}

@Keep
@IgnoreExtraProperties
data class DailyExpense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateText: String = "",
    val category: String = "অটো ভাড়া",
    val description: String = "",
    val amount: Double = 0.0,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TrashItemType {
    CARTON,
    GRADE
}

@Keep
@IgnoreExtraProperties
data class TrashItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = TrashItemType.CARTON.name,
    val gradeName: String = "",
    val weightKg: Double = 0.0,
    val ratePerKg: Double = 0.0,
    val cartonWeights: List<Double> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
