package com.example

import com.example.data.model.CalculatorState
import com.example.data.model.GradeEntry
import com.example.util.BengaliFormatter
import com.example.util.WhatsAppExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorAndFormatterTest {

    @Test
    fun `calculator computes grade subtotals, gross sales, commission, and net payable accurately`() {
        val state = CalculatorState(
            dateText = "2026-11-17",
            displayDate = "১৭ নভেম্বর, ২০২৬",
            aratName = "স্ট্রবেরি সেন্ট্রাল আড়ত",
            supplierName = "করিম মিয়া",
            grades = listOf(
                GradeEntry("g1", "SS VIP", ratePerKg = 500.0, cartonWeights = listOf(2.5, 3.0)),
                GradeEntry("g2", "VIP", ratePerKg = 400.0, cartonWeights = listOf(2.0, 2.5, 3.0)),
                GradeEntry("g3", "Cat (ক্যাট)", ratePerKg = 200.0, cartonWeights = emptyList())
            ),
            commissionPercent = 7.0,
            kuliExpense = 150.0,
            otherExpense = 50.0
        )

        assertEquals(5L, state.totalCartons)
        assertEquals(13.0, state.totalWeightKg, 0.001)
        // 5.5 * 500 = 2750; 7.5 * 400 = 3000 => 5750
        assertEquals(5750.0, state.grossSales, 0.001)
        // 7% of 5750 = 402.5
        assertEquals(402.5, state.commissionAmount, 0.001)
        // 402.5 + 150 + 50 = 602.5
        assertEquals(602.5, state.totalDeductions, 0.001)
        // 5750 - 602.5 = 5147.5
        assertEquals(5147.5, state.netPayable, 0.001)
    }

    @Test
    fun `bengali formatter parses both ascii and bengali numerals`() {
        assertEquals(2.75, BengaliFormatter.parseNumericInput("২.৭৫") ?: 0.0, 0.001)
        assertEquals(450.0, BengaliFormatter.parseNumericInput("450") ?: 0.0, 0.001)
        assertEquals("৳ ১,২৫০", BengaliFormatter.formatTaka(1250.0))
        assertEquals("৫.৫ কেজি", BengaliFormatter.formatKg(5.5))
    }

    @Test
    fun `whatsapp report formats bengali summary cleanly`() {
        val state = CalculatorState(
            dateText = "2026-11-17",
            displayDate = "১৭ নভেম্বর, ২০২৬",
            aratName = "সোনার বাংলা আড়ত",
            supplierName = "রহিম",
            grades = listOf(GradeEntry("g1", "SS VIP", 500.0, listOf(2.5, 2.5))),
            commissionPercent = 7.0,
            kuliExpense = 100.0,
            chalanNote = "ঢাকা মেট্রো-ট ১১-২২৩৩"
        )
        val msg = WhatsAppExporter.buildCalculatorReport(state)
        assertTrue(msg.contains("সোনার বাংলা আড়ত"))
        assertTrue(msg.contains("SS VIP"))
        assertTrue(msg.contains("মালিকের নিট পাওনা"))
        assertTrue(msg.contains("ঢাকা মেট্রো-ট ১১-২২৩৩"))
    }
}
