package com.example.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyExpense
import com.example.data.model.UiState
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.CrateAmber
import com.example.ui.theme.CrateAmberTint
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.StrawberrySoftBg
import com.example.util.BengaliFormatter

@Composable
fun ExpensesScreen(
    expensesState: UiState<List<DailyExpense>>,
    onSaveExpense: (String?, String, String, Double) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<DailyExpense?>(null) }

    when (expensesState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StrawberryRed)
            }
        }
        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(expensesState.message, color = StrawberryRed)
            }
        }
        is UiState.Success -> {
            val activeExpenses = expensesState.data.filter { !it.deleted }
            val todayIso = BengaliFormatter.currentDateIso()
            val todayTotal = activeExpenses.filter { it.dateText == todayIso }.sumOf { it.amount }
            val grandTotal = activeExpenses.sumOf { it.amount }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Summary Bento
                item(key = "expense_hero") {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(EmeraldGreen, EmeraldDark)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "আজকের হাতখরচ",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = BengaliFormatter.formatTaka(todayTotal),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "মোট সংরক্ষিত খরচ (${BengaliFormatter.formatInt(activeExpenses.size)} টি): ${BengaliFormatter.formatTaka(grandTotal)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                item(key = "add_expense_btn") {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StrawberryRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_expense_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন হাতখরচ যোগ করুন", style = MaterialTheme.typography.titleSmall)
                    }
                }

                if (activeExpenses.isEmpty()) {
                    item(key = "empty_expenses") {
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
                                    Icons.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = StrawberryRed.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "এখনো কোনো হাতখরচ যোগ করা হয়নি",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "অটো ভাড়া, নাস্তা বা কুলি খরচ সহজে লিখে রাখুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(items = activeExpenses, key = { it.id }) { expense ->
                        ExpenseRowCard(
                            expense = expense,
                            onEdit = { editingExpense = expense },
                            onDelete = { onDeleteExpense(expense.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ExpenseDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, desc, amt ->
                onSaveExpense(null, cat, desc, amt)
                showAddDialog = false
            }
        )
    }

    editingExpense?.let { exp ->
        ExpenseDialog(
            initial = exp,
            onDismiss = { editingExpense = null },
            onConfirm = { cat, desc, amt ->
                onSaveExpense(exp.id, cat, desc, amt)
                editingExpense = null
            }
        )
    }
}

@Composable
private fun ExpenseRowCard(
    expense: DailyExpense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BentoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CrateAmberTint
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = CrateAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = expense.dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expense.description.isNotBlank()) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = BengaliFormatter.formatTaka(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = StrawberryRed,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "খরচ সম্পাদনা",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "রিসাইকেল বিনে পাঠান",
                        tint = StrawberryRed,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseDialog(
    initial: DailyExpense?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    val categories = listOf("অটো ভাড়া", "নাস্তা", "কুলি/লেবার", "মোবাইল/নেট", "অন্যান্য")
    var selectedCategory by remember { mutableStateOf(initial?.category ?: categories.first()) }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (initial != null && initial.amount > 0.0) initial.amount.toBigDecimal().stripTrailingZeros().toPlainString()
            else ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "নতুন হাতখরচ যোগ" else "হাতখরচ সম্পাদনা")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("খরচের খাত বেছে নিন:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldTint,
                                selectedLabelColor = EmeraldDark
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("টাকার পরিমাণ (৳)") },
                    placeholder = { Text("যেমন: ১৫০") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বিবরণ (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: বাজার থেকে আড়ত ভ্যান ভাড়া") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Surface(color = StrawberrySoftBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            it,
                            color = StrawberryRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = BengaliFormatter.parseNumericInput(amountText)
                    if (amt == null || amt <= 0.0) {
                        error = "সঠিক টাকার পরিমাণ দিন।"
                    } else {
                        onConfirm(selectedCategory, description.trim(), amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
