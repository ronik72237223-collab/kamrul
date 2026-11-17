package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.model.AratLedger
import com.example.data.model.CalculatorState

object WhatsAppExporter {

    fun buildCalculatorReport(state: CalculatorState): String {
        val sb = StringBuilder()
        sb.appendLine("🍓 *${state.aratName.ifBlank { "স্ট্রবেরি আড়ত প্রো" }}*")
        sb.appendLine("📅 তারিখ: ${state.displayDate}")
        if (state.supplierName.isNotBlank()) {
            sb.appendLine("👤 মালিক/মহাজন: ${state.supplierName}")
        }
        sb.appendLine("━━━━━━━━━━━━━━━")
        sb.appendLine("📦 *কোয়ালিটি অনুযায়ী হিসাব:*")

        var anyGrade = false
        state.grades.forEach { grade ->
            if (grade.totalCartons > 0 || grade.ratePerKg > 0.0) {
                anyGrade = true
                sb.appendLine(
                    "• *${grade.name}*: ${BengaliFormatter.formatInt(grade.totalCartons)} কার্টুন | " +
                        "${BengaliFormatter.formatKg(grade.totalWeightKg)} × ${BengaliFormatter.formatTaka(grade.ratePerKg)} = *${BengaliFormatter.formatTaka(grade.subtotalSales)}*"
                )
            }
        }
        if (!anyGrade) {
            sb.appendLine("• কোনো কার্টুন এন্ট্রি নেই")
        }

        sb.appendLine("━━━━━━━━━━━━━━━")
        sb.appendLine("📊 সর্বমোট কার্টুন: *${BengaliFormatter.formatInt(state.totalCartons)} টি*")
        sb.appendLine("⚖️ মোট নিট ওজন: *${BengaliFormatter.formatKg(state.totalWeightKg)}*")
        sb.appendLine("💰 মোট বিক্রয়মূল্য: *${BengaliFormatter.formatTaka(state.grossSales)}*")
        sb.appendLine("➖ আড়ত কমিশন (${BengaliFormatter.formatNumber(state.commissionPercent)}%): ${BengaliFormatter.formatTaka(state.commissionAmount)}")
        if (state.kuliExpense > 0.0) {
            sb.appendLine("➖ কুলি খরচ: ${BengaliFormatter.formatTaka(state.kuliExpense)}")
        }
        if (state.otherExpense > 0.0) {
            sb.appendLine("➖ অন্যান্য খরচ: ${BengaliFormatter.formatTaka(state.otherExpense)}")
        }
        sb.appendLine("━━━━━━━━━━━━━━━")
        sb.appendLine("✅ *মালিকের নিট পাওনা: ${BengaliFormatter.formatTaka(state.netPayable)}*")
        sb.appendLine("💳 পেমেন্ট: ${state.paymentMethod}")
        if (state.chalanNote.isNotBlank()) {
            sb.appendLine("🚛 চালান নোট: ${state.chalanNote}")
        }
        if (state.signatureText.isNotBlank()) {
            sb.appendLine("✍️ স্বাক্ষর: ${state.signatureText}")
        }
        return sb.toString().trim()
    }

    fun buildLedgerReport(ledger: AratLedger): String {
        return buildCalculatorReport(
            CalculatorState(
                dateText = ledger.dateText,
                displayDate = ledger.displayDate,
                aratName = ledger.aratName,
                supplierName = ledger.supplierName,
                grades = ledger.grades,
                commissionPercent = ledger.commissionPercent,
                kuliExpense = ledger.kuliExpense,
                otherExpense = ledger.otherExpense,
                chalanNote = ledger.chalanNote,
                paymentMethod = ledger.paymentMethod,
                signatureText = ledger.signatureText
            )
        )
    }

    fun shareToWhatsApp(context: Context, message: String) {
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        val targetPkg = packages.firstOrNull { isPackageInstalled(context, it) }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            if (targetPkg != null) {
                setPackage(targetPkg)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (targetPkg != null) {
                context.startActivity(sendIntent)
            } else {
                val chooser = Intent.createChooser(sendIntent, "হোয়াটসঅ্যাপে রিপোর্ট পাঠান").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (_: Exception) {
            val fallback = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                },
                "রিপোর্ট শেয়ার করুন"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
