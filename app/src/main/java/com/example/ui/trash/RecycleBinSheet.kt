package com.example.ui.trash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AratLedger
import com.example.data.model.DailyExpense
import com.example.data.model.TrashItem
import com.example.data.model.TrashItemType
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.StrawberrySoftBg
import com.example.util.BengaliFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinSheet(
    deletedLedgers: List<AratLedger>,
    deletedExpenses: List<DailyExpense>,
    trashItems: List<TrashItem>,
    onRestoreLedger: (String) -> Unit,
    onDeleteLedgerPermanently: (String) -> Unit,
    onRestoreExpense: (String) -> Unit,
    onDeleteExpensePermanently: (String) -> Unit,
    onRestoreTrashItem: (TrashItem) -> Unit,
    onDeleteTrashItemPermanently: (String) -> Unit,
    onEmptyBin: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalCount = deletedLedgers.size + deletedExpenses.size + trashItems.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BentoBackground,
        contentColor = SlateInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "রিসাইকেল বিন (Trash Bin)",
                        style = MaterialTheme.typography.titleLarge,
                        color = SlateInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "মুছে ফেলা সব ডাটা এখান থেকে ১ ট্যাপে ফিরিয়ে আনুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }

                if (totalCount > 0) {
                    TextButton(onClick = onEmptyBin) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = StrawberryRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সব খালি", color = StrawberryRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (totalCount == 0) {
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
                            Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = EmeraldGreen.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "রিসাইকেল বিন খালি আছে",
                            style = MaterialTheme.typography.titleSmall,
                            color = SlateInk
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = trashItems, key = { "trash_${it.id}" }) { item ->
                        val title = if (item.itemType == TrashItemType.GRADE.name) {
                            "কোয়ালিটি: ${item.gradeName} (${BengaliFormatter.formatInt(item.cartonWeights.size)} কার্টুন)"
                        } else {
                            "${item.gradeName} — ${BengaliFormatter.formatKg(item.weightKg)}"
                        }
                        TrashEntryCard(
                            badge = if (item.itemType == TrashItemType.GRADE.name) "কোয়ালিটি গ্রেড" else "কার্টুন ওজন",
                            title = title,
                            subtitle = "দর: ${BengaliFormatter.formatTaka(item.ratePerKg)}/কেজি",
                            onRestore = { onRestoreTrashItem(item) },
                            onDeleteForever = { onDeleteTrashItemPermanently(item.id) }
                        )
                    }

                    items(items = deletedLedgers, key = { "ledger_${it.id}" }) { ledger ->
                        TrashEntryCard(
                            badge = "খাতার হিসাব",
                            title = "${ledger.supplierName.ifBlank { ledger.aratName }} (${ledger.displayDate})",
                            subtitle = "${BengaliFormatter.formatInt(ledger.totalCartons)} কার্টুন • নিট পাওনা: ${BengaliFormatter.formatTaka(ledger.netPayable)}",
                            onRestore = { onRestoreLedger(ledger.id) },
                            onDeleteForever = { onDeleteLedgerPermanently(ledger.id) }
                        )
                    }

                    items(items = deletedExpenses, key = { "exp_${it.id}" }) { exp ->
                        TrashEntryCard(
                            badge = "হাতখরচ",
                            title = "${exp.category} — ${BengaliFormatter.formatTaka(exp.amount)}",
                            subtitle = exp.description.ifBlank { exp.dateText },
                            onRestore = { onRestoreExpense(exp.id) },
                            onDeleteForever = { onDeleteExpensePermanently(exp.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashEntryCard(
    badge: String,
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
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
                Surface(shape = RoundedCornerShape(8.dp), color = StrawberrySoftBg) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = StrawberryRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteForever,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Rounded.DeleteForever,
                        contentDescription = "স্থায়ীভাবে মুছুন",
                        tint = StrawberryRed
                    )
                }
            }

            Text(title, style = MaterialTheme.typography.titleSmall, color = SlateInk, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SlateMuted)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestore,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ফিরিয়ে আনুন", color = Color.White)
                }

                OutlinedButton(
                    onClick = onDeleteForever,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("স্থায়ী মুছুন", color = StrawberryRed)
                }
            }
        }
    }
}
