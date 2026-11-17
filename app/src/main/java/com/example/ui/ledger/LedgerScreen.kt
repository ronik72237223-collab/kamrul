package com.example.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AratLedger
import com.example.data.model.UiState
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.StrawberrySoftBg
import com.example.ui.theme.appTextFieldColors
import com.example.util.BengaliFormatter
import com.example.util.WhatsAppExporter

@Composable
fun LedgerScreen(
    ledgersState: UiState<List<AratLedger>>,
    isExporting: Boolean,
    onExportExcel: (List<AratLedger>) -> Unit,
    onEditLedger: (AratLedger) -> Unit,
    onDeleteLedger: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    when (ledgersState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StrawberryRed)
            }
        }
        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(ledgersState.message, color = StrawberryRed)
            }
        }
        is UiState.Success -> {
            val activeLedgers = ledgersState.data.filter { !it.deleted }
            val filtered = remember(activeLedgers, searchQuery) {
                if (searchQuery.isBlank()) activeLedgers
                else {
                    val q = searchQuery.trim().lowercase()
                    activeLedgers.filter {
                        it.supplierName.lowercase().contains(q) ||
                            it.aratName.lowercase().contains(q) ||
                            it.displayDate.lowercase().contains(q) ||
                            it.dateText.lowercase().contains(q)
                    }
                }
            }

            val totalCartons = filtered.sumOf { it.totalCartons }
            val totalWeight = filtered.sumOf { it.totalWeightKg }
            val totalNet = filtered.sumOf { it.netPayable }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Summary Strip + 1-Tap Excel Download Button
                item(key = "ledger_header") {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "হিসাব খাতা ও এক্সেল রিপোর্ট",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SlateInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${BengaliFormatter.formatInt(filtered.size)} টি হিসাব • ${BengaliFormatter.formatInt(totalCartons)} কার্টুন • ${BengaliFormatter.formatKg(totalWeight)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateMuted
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldSoftBg
                                ) {
                                    Text(
                                        text = BengaliFormatter.formatTaka(totalNet),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = EmeraldDark,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { onExportExcel(filtered) },
                                enabled = !isExporting,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("download_excel_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isExporting) "এক্সেল তৈরি হচ্ছে..." else "এক্সেল ডাউনলোড করুন (.xlsx)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 2. Search Bar
                item(key = "ledger_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("মালিকের নাম, আড়ত বা তারিখ দিয়ে খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (filtered.isEmpty()) {
                    item(key = "empty_ledger") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BentoBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Book,
                                    contentDescription = null,
                                    tint = EmeraldGreen.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "খাতায় এখনো কোনো হিসাব সংরক্ষিত নেই",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SlateInk
                                )
                                Text(
                                    text = "আজকের হিসাব ট্যাব থেকে 'খাতায় সেভ' বাটনে চাপলে এখানে চলে আসবে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                } else {
                    // 3. Horizontally Scrollable Accounting Book Table
                    item(key = "ledger_table") {
                        AccountingBookTable(
                            ledgers = filtered,
                            onEdit = onEditLedger,
                            onShare = { ledger ->
                                WhatsAppExporter.shareToWhatsApp(
                                    context,
                                    WhatsAppExporter.buildLedgerReport(ledger)
                                )
                            },
                            onDelete = { ledger -> onDeleteLedger(ledger.id) }
                        )
                    }

                    // 4. Quick Mobile Summary Cards below table
                    items(items = filtered, key = { it.id }) { ledger ->
                        LedgerQuickCard(
                            ledger = ledger,
                            onEdit = { onEditLedger(ledger) },
                            onShare = {
                                WhatsAppExporter.shareToWhatsApp(
                                    context,
                                    WhatsAppExporter.buildLedgerReport(ledger)
                                )
                            },
                            onDelete = { onDeleteLedger(ledger.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountingBookTable(
    ledgers: List<AratLedger>,
    onEdit: (AratLedger) -> Unit,
    onShare: (AratLedger) -> Unit,
    onDelete: (AratLedger) -> Unit
) {
    val scrollState = rememberScrollState()
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BentoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ডিজিটাল হিসাব খাতা (বাম-ডানে স্ক্রল করুন)",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                // Header
                Row(
                    modifier = Modifier
                        .background(EmeraldGreen, RoundedCornerShape(10.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeaderCell("তারিখ", 110.dp)
                    TableHeaderCell("আড়তের নাম", 130.dp)
                    TableHeaderCell("মালিকের নাম", 120.dp)
                    TableHeaderCell("কোয়ালিটি বিবরণ", 210.dp)
                    TableHeaderCell("মোট কার্টুন", 80.dp)
                    TableHeaderCell("নিট ওজন", 95.dp)
                    TableHeaderCell("গড় দর", 85.dp)
                    TableHeaderCell("মোট বিক্রয়", 100.dp)
                    TableHeaderCell("কর্তন", 95.dp)
                    TableHeaderCell("নিট পাওনা", 110.dp)
                    TableHeaderCell("পেমেন্ট", 100.dp)
                    TableHeaderCell("স্বাক্ষর", 100.dp)
                    TableHeaderCell("অ্যাকশন", 124.dp)
                }

                ledgers.forEachIndexed { idx, ledger ->
                    val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC)
                    Row(
                        modifier = Modifier
                            .background(rowBg)
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableBodyCell(ledger.displayDate.ifBlank { ledger.dateText }, 110.dp)
                        TableBodyCell(ledger.aratName, 130.dp)
                        TableBodyCell(ledger.supplierName.ifBlank { "-" }, 120.dp)
                        TableBodyCell(ledger.qualityBreakdownText(), 210.dp)
                        TableBodyCell("${BengaliFormatter.formatInt(ledger.totalCartons)} টি", 80.dp)
                        TableBodyCell(BengaliFormatter.formatKg(ledger.totalWeightKg), 95.dp)
                        TableBodyCell(BengaliFormatter.formatTaka(ledger.averageRate), 85.dp)
                        TableBodyCell(BengaliFormatter.formatTaka(ledger.grossSales), 100.dp)
                        TableBodyCell(BengaliFormatter.formatTaka(ledger.totalDeductions), 95.dp)
                        TableBodyCell(
                            text = BengaliFormatter.formatTaka(ledger.netPayable),
                            width = 110.dp,
                            color = EmeraldDark,
                            bold = true
                        )
                        TableBodyCell(ledger.paymentMethod, 100.dp)
                        TableBodyCell(ledger.signatureText.ifBlank { "-" }, 100.dp)
                        Row(
                            modifier = Modifier.width(124.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onEdit(ledger) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "সম্পাদনা", tint = SlateMuted, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onShare(ledger) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "শেয়ার", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onDelete(ledger) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "মুছুন", tint = StrawberryRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = BentoBorder)
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
    )
}

@Composable
private fun TableBodyCell(
    text: String,
    width: Dp,
    color: Color = SlateInk,
    bold: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
    )
}

@Composable
private fun LedgerQuickCard(
    ledger: AratLedger,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BentoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ledger.supplierName.ifBlank { ledger.aratName },
                        style = MaterialTheme.typography.titleSmall,
                        color = SlateInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ledger.displayDate} • ${ledger.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }
                Surface(shape = RoundedCornerShape(10.dp), color = StrawberrySoftBg) {
                    Text(
                        text = BengaliFormatter.formatTaka(ledger.netPayable),
                        style = MaterialTheme.typography.titleSmall,
                        color = StrawberryRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = ledger.qualityBreakdownText(),
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${BengaliFormatter.formatInt(ledger.totalCartons)} কার্টুন • ${BengaliFormatter.formatKg(ledger.totalWeightKg)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldDark
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Edit, contentDescription = "সম্পাদনা", tint = SlateMuted, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Share, contentDescription = "হোয়াটসঅ্যাপ", tint = EmeraldGreen, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "মুছুন", tint = StrawberryRed, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}
