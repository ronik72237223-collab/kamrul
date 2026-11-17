package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.AratLedger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExportResult(
    val uri: Uri,
    val fileName: String,
    val displayLocation: String
)

object ExcelExporter {

    private const val XLSX_MIME =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    suspend fun exportLedgersToXlsx(
        context: Context,
        ledgers: List<AratLedger>
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val fileName = "Strawberry_Arat_Ledger_$stamp.xlsx"
            val xlsxBytes = buildXlsxBytes(ledgers)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, XLSX_MIME)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("ডাউনলোড ফোল্ডারে ফাইল তৈরি করা যায়নি।")

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(xlsxBytes)
                    out.flush()
                } ?: error("ফাইল লেখা যায়নি।")

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Result.success(
                    ExportResult(
                        uri = uri,
                        fileName = fileName,
                        displayLocation = "Downloads/$fileName"
                    )
                )
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: File(context.cacheDir, "exports").apply { mkdirs() }
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    out.write(xlsxBytes)
                    out.flush()
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                Result.success(
                    ExportResult(
                        uri = uri,
                        fileName = fileName,
                        displayLocation = file.absolutePath
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openOrShareExcel(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "এক্সেল ফাইল শেয়ার বা ওপেন করুন").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun buildXlsxBytes(ledgers: List<AratLedger>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml(ledgers))
        }
        return baos.toByteArray()
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun colName(index: Int): String {
        var idx = index
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.insert(0, ('A' + (idx % 26)))
            idx = (idx / 26) - 1
        }
        return sb.toString()
    }

    private fun stringCell(row: Int, col: Int, value: String, style: Int = 0): String {
        val ref = "${colName(col)}$row"
        return "<c r=\"$ref\" t=\"inlineStr\" s=\"$style\"><is><t>${escapeXml(value)}</t></is></c>"
    }

    private fun numberCell(row: Int, col: Int, value: Double, style: Int = 2): String {
        val ref = "${colName(col)}$row"
        val clean = if (value.isNaN() || value.isInfinite()) 0.0 else value
        return "<c r=\"$ref\" s=\"$style\"><v>$clean</v></c>"
    }

    private fun sheetXml(ledgers: List<AratLedger>): String {
        val headers = listOf(
            "তারিখ (Date)",
            "আড়তের নাম (Arat Name)",
            "মালিকের নাম (Supplier)",
            "কোয়ালিটি বিবরণ (Quality Breakdown)",
            "মোট কার্টুন (Cartons)",
            "নিট ওজন কেজি (Net Weight)",
            "গড় দর ৳/কেজি (Avg Rate)",
            "মোট বিক্রয় ৳ (Gross Sales)",
            "কমিশন ৳ (Commission)",
            "মোট কর্তন ৳ (Deductions)",
            "নিট পাওনা ৳ (Net Payable)",
            "পেমেন্ট (Payment)",
            "চালান নোট (Chalan Note)",
            "স্বাক্ষর (Signature)"
        )

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<cols>")
        val widths = listOf(16, 22, 18, 42, 14, 16, 15, 18, 16, 16, 18, 16, 26, 16)
        widths.forEachIndexed { i, w ->
            sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
        }
        sb.append("</cols>")
        sb.append("<sheetData>")

        // Header row (Row 1, style 1 = emerald bold white)
        sb.append("<row r=\"1\" ht=\"26\" customHeight=\"1\">")
        headers.forEachIndexed { col, title ->
            sb.append(stringCell(1, col, title, style = 1))
        }
        sb.append("</row>")

        var sumCartons = 0L
        var sumWeight = 0.0
        var sumGross = 0.0
        var sumComm = 0.0
        var sumDeductions = 0.0
        var sumNet = 0.0

        ledgers.forEachIndexed { idx, ledger ->
            val r = idx + 2
            sumCartons += ledger.totalCartons
            sumWeight += ledger.totalWeightKg
            sumGross += ledger.grossSales
            sumComm += ledger.commissionAmount
            sumDeductions += ledger.totalDeductions
            sumNet += ledger.netPayable

            sb.append("<row r=\"$r\" ht=\"22\" customHeight=\"1\">")
            sb.append(stringCell(r, 0, ledger.displayDate.ifBlank { ledger.dateText }, style = 0))
            sb.append(stringCell(r, 1, ledger.aratName, style = 0))
            sb.append(stringCell(r, 2, ledger.supplierName.ifBlank { "-" }, style = 0))
            sb.append(stringCell(r, 3, ledger.qualityBreakdownText(), style = 0))
            sb.append(numberCell(r, 4, ledger.totalCartons.toDouble(), style = 2))
            sb.append(numberCell(r, 5, ledger.totalWeightKg, style = 2))
            sb.append(numberCell(r, 6, ledger.averageRate, style = 2))
            sb.append(numberCell(r, 7, ledger.grossSales, style = 2))
            sb.append(numberCell(r, 8, ledger.commissionAmount, style = 2))
            sb.append(numberCell(r, 9, ledger.totalDeductions, style = 2))
            sb.append(numberCell(r, 10, ledger.netPayable, style = 3))
            sb.append(stringCell(r, 11, ledger.paymentMethod, style = 0))
            sb.append(stringCell(r, 12, ledger.chalanNote.ifBlank { "-" }, style = 0))
            sb.append(stringCell(r, 13, ledger.signatureText.ifBlank { "-" }, style = 0))
            sb.append("</row>")
        }

        // Totals row
        val totalRow = ledgers.size + 2
        sb.append("<row r=\"$totalRow\" ht=\"24\" customHeight=\"1\">")
        sb.append(stringCell(totalRow, 0, "সর্বমোট (Total)", style = 3))
        sb.append(stringCell(totalRow, 1, "${ledgers.size} টি হিসাব", style = 3))
        sb.append(stringCell(totalRow, 2, "", style = 3))
        sb.append(stringCell(totalRow, 3, "", style = 3))
        sb.append(numberCell(totalRow, 4, sumCartons.toDouble(), style = 3))
        sb.append(numberCell(totalRow, 5, sumWeight, style = 3))
        sb.append(numberCell(totalRow, 6, if (sumWeight > 0) sumGross / sumWeight else 0.0, style = 3))
        sb.append(numberCell(totalRow, 7, sumGross, style = 3))
        sb.append(numberCell(totalRow, 8, sumComm, style = 3))
        sb.append(numberCell(totalRow, 9, sumDeductions, style = 3))
        sb.append(numberCell(totalRow, 10, sumNet, style = 3))
        sb.append(stringCell(totalRow, 11, "", style = 3))
        sb.append(stringCell(totalRow, 12, "", style = 3))
        sb.append(stringCell(totalRow, 13, "", style = 3))
        sb.append("</row>")

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Arat Ledger" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.##"/>
  </numFmts>
  <fonts count="3">
    <font><sz val="11"/><color rgb="FF0F172A"/><name val="Arial"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
    <font><b/><sz val="11"/><color rgb="FF065F46"/><name val="Arial"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF059669"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFD1FAE5"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFE2E8F0"/></left>
      <right style="thin"><color rgb="FFE2E8F0"/></right>
      <top style="thin"><color rgb="FFE2E8F0"/></top>
      <bottom style="thin"><color rgb="FFE2E8F0"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="4">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="164" fontId="2" fillId="3" borderId="1" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  </cellXfs>
</styleSheet>"""
}
