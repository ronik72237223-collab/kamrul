package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object BengaliFormatter {
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val bnMonths = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val decimalFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
    private val intFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(value: Double): String {
        val formatted = if (abs(value - value.roundToLong()) < 0.005) {
            intFormat.format(value.roundToLong())
        } else {
            decimalFormat.format(value)
        }
        return toBengaliDigits(formatted)
    }

    fun formatInt(value: Long): String = toBengaliDigits(intFormat.format(value))

    fun formatInt(value: Int): String = formatInt(value.toLong())

    fun formatTaka(amount: Double): String {
        val prefix = if (amount < 0) "-৳ " else "৳ "
        return prefix + formatNumber(abs(amount))
    }

    fun formatKg(kg: Double): String = "${formatNumber(kg)} কেজি"

    fun currentDateIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun currentDateBengali(): String {
        val cal = java.util.Calendar.getInstance()
        val day = toBengaliDigits(cal.get(java.util.Calendar.DAY_OF_MONTH).toString())
        val month = bnMonths[cal.get(java.util.Calendar.MONTH)]
        val year = toBengaliDigits(cal.get(java.util.Calendar.YEAR).toString())
        return "$day $month, $year"
    }

    fun parseNumericInput(text: String): Double? {
        // Normalize Bengali digits to ASCII digits before parsing
        val sb = StringBuilder(text.length)
        for (ch in text.trim()) {
            val idx = bnDigits.indexOf(ch)
            when {
                idx >= 0 -> sb.append(('0' + idx))
                ch in '0'..'9' || ch == '.' -> sb.append(ch)
            }
        }
        return sb.toString().toDoubleOrNull()
    }
}
