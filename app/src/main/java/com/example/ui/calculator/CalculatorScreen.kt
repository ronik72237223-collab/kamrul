package com.example.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.CalculatorState
import com.example.data.model.GradeEntry
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.CrateAmber
import com.example.ui.theme.CrateAmberTint
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StrawberryDark
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.StrawberrySoftBg
import com.example.ui.theme.StrawberryTint
import com.example.ui.theme.appTextFieldColors
import com.example.util.BengaliFormatter
import com.example.util.WhatsAppExporter

private data class CartonEditTarget(
    val gradeId: String,
    val gradeName: String,
    val index: Int?, // null = add new carton
    val currentWeight: Double
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    state: CalculatorState,
    appConfig: AppConfig,
    isSaving: Boolean,
    onAddCarton: (String, Double) -> Unit,
    onEditCarton: (String, Int, Double) -> Unit,
    onDeleteCarton: (String, Int) -> Unit,
    onUpdateGradeRate: (String, Double) -> Unit,
    onAddCustomGrade: (String, Double) -> Unit,
    onRenameGrade: (String, String, Double) -> Unit,
    onDeleteGrade: (String) -> Unit,
    onUpdateDeductions: (Double, Double, Double) -> Unit,
    onUpdateMetadata: (String, String, String, String, String) -> Unit,
    onSaveLedger: (Boolean) -> Unit,
    onStartNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var cartonDialogTarget by remember { mutableStateOf<CartonEditTarget?>(null) }
    var showAddGradeDialog by remember { mutableStateOf(false) }
    var gradeToEdit by remember { mutableStateOf<GradeEntry?>(null) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dynamic Welcome Banner from Admin
        if (appConfig.welcomeBanner.isNotBlank()) {
            item(key = "welcome_banner") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StrawberrySoftBg,
                    border = BorderStroke(1.dp, StrawberryTint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = "নোটিশ",
                            tint = StrawberryRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = appConfig.welcomeBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StrawberryDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Editing Badge if editing an older ledger
        if (state.editingLedgerId != null) {
            item(key = "editing_banner") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CrateAmberTint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "খাতার পুরোনো হিসাব সম্পাদনা করছেন",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrateAmber
                        )
                        TextButton(onClick = onStartNew) {
                            Text("বাতিল করুন")
                        }
                    }
                }
            }
        }

        // 3. Hero Net Payable Bento Card + 3 Quick Metric Tiles
        item(key = "hero_summary") {
            HeroSummaryBento(state = state)
        }

        // 4. Net Weight Note Badge
        item(key = "net_weight_note") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "কোয়ালিটি অনুযায়ী কার্টুন হিসাব",
                    style = MaterialTheme.typography.titleMedium,
                    color = SlateInk
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = EmeraldSoftBg,
                    border = BorderStroke(1.dp, EmeraldTint)
                ) {
                    Text(
                        text = "খালি বক্স বাদ ছাড়াই নিট ওজন",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 5. Quality Grade Bento Cards
        items(items = state.grades, key = { it.gradeId }) { grade ->
            QualityGradeCard(
                grade = grade,
                onQuickAddCarton = {
                    cartonDialogTarget = CartonEditTarget(
                        gradeId = grade.gradeId,
                        gradeName = grade.name,
                        index = null,
                        currentWeight = 0.0
                    )
                },
                onEditCarton = { idx, w ->
                    cartonDialogTarget = CartonEditTarget(
                        gradeId = grade.gradeId,
                        gradeName = grade.name,
                        index = idx,
                        currentWeight = w
                    )
                },
                onDeleteCarton = { idx -> onDeleteCarton(grade.gradeId, idx) },
                onRateChange = { rate -> onUpdateGradeRate(grade.gradeId, rate) },
                onEditGrade = { gradeToEdit = grade },
                onDeleteGrade = { onDeleteGrade(grade.gradeId) }
            )
        }

        // 6. Add Custom Quality Grade Button
        item(key = "add_grade_btn") {
            OutlinedButton(
                onClick = { showAddGradeDialog = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, StrawberryRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_custom_grade_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = StrawberryRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+ নতুন কোয়ালিটি যোগ করুন",
                    style = MaterialTheme.typography.titleSmall,
                    color = StrawberryRed
                )
            }
        }

        // 7. Deductions Card (Commission % & Kuli/Other)
        item(key = "deductions_card") {
            DeductionsBentoCard(
                state = state,
                onUpdateDeductions = onUpdateDeductions
            )
        }

        // 8. Chalan Note, Supplier, Payment Method & Signature Card
        item(key = "chalan_metadata_card") {
            ChalanMetadataCard(
                state = state,
                onUpdateMetadata = onUpdateMetadata
            )
        }

        // 9. Action Buttons: Save to Khata, WhatsApp Share, New Calculation
        item(key = "bottom_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (state.editingLedgerId != null) {
                                onSaveLedger(false)
                            } else {
                                showSaveConfirmDialog = true
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("save_ledger_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.editingLedgerId != null) "আপডেট সেভ" else "খাতায় সেভ",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = {
                            val report = WhatsAppExporter.buildCalculatorReport(state)
                            WhatsAppExporter.shareToWhatsApp(context, report)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("whatsapp_share_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "হোয়াটসঅ্যাপ",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                OutlinedButton(
                    onClick = onStartNew,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("new_calculation_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = SlateInk, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("নতুন হিসাব শুরু করুন", color = SlateInk)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Carton Add / Edit Dialog
    cartonDialogTarget?.let { target ->
        CartonWeightDialog(
            target = target,
            onDismiss = { cartonDialogTarget = null },
            onConfirm = { weight ->
                if (target.index == null) {
                    onAddCarton(target.gradeId, weight)
                } else {
                    onEditCarton(target.gradeId, target.index, weight)
                }
                cartonDialogTarget = null
            }
        )
    }

    // Add Custom Grade Dialog
    if (showAddGradeDialog) {
        GradeDialog(
            initialName = "",
            initialRate = 0.0,
            title = "নতুন কোয়ালিটি গ্রেড যোগ করুন",
            confirmText = "যোগ করুন",
            onDismiss = { showAddGradeDialog = false },
            onConfirm = { name, rate ->
                onAddCustomGrade(name, rate)
                showAddGradeDialog = false
            }
        )
    }

    // Edit Grade Dialog
    gradeToEdit?.let { g ->
        GradeDialog(
            initialName = g.name,
            initialRate = g.ratePerKg,
            title = "কোয়ালিটি সম্পাদনা করুন",
            confirmText = "আপডেট করুন",
            onDismiss = { gradeToEdit = null },
            onConfirm = { name, rate ->
                onRenameGrade(g.gradeId, name, rate)
                gradeToEdit = null
            }
        )
    }

    // Save Confirmation Dialog
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            containerColor = Color.White,
            titleContentColor = SlateInk,
            textContentColor = SlateMuted,
            title = { Text("খাতায় হিসাব সংরক্ষণ", color = SlateInk) },
            text = {
                Text(
                    "সংরক্ষণ করার পর কি এই হিসাব পরিষ্কার করে নতুন চালানের হিসাব শুরু করতে চান?",
                    color = SlateMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveConfirmDialog = false
                        onSaveLedger(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("সেভ ও নতুন শুরু", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveConfirmDialog = false
                        onSaveLedger(false)
                    }
                ) {
                    Text("শুধু সেভ করুন", color = SlateInk)
                }
            }
        )
    }
}

@Composable
private fun HeroSummaryBento(state: CalculatorState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Big Highlighted Net Payable Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StrawberryRed),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(StrawberryRed, Color(0xFFB80222))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মালিকের নিট পাওনা টাকা",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = state.displayDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = BengaliFormatter.formatTaka(state.netPayable),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("net_payable_value")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "মোট বিক্রয়: ${BengaliFormatter.formatTaka(state.grossSales)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.94f)
                        )
                        Text(
                            text = "মোট কর্তন: ${BengaliFormatter.formatTaka(state.totalDeductions)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.94f)
                        )
                    }
                }
            }
        }

        // 3 Mini Bento Metric Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoStatTile(
                title = "মোট কার্টুন",
                value = "${BengaliFormatter.formatInt(state.totalCartons)} টি",
                icon = Icons.Rounded.Inventory2,
                tint = StrawberryRed,
                bg = Color.White,
                modifier = Modifier.weight(1f)
            )
            BentoStatTile(
                title = "নিট ওজন",
                value = BengaliFormatter.formatKg(state.totalWeightKg),
                icon = Icons.Rounded.Scale,
                tint = EmeraldGreen,
                bg = Color.White,
                modifier = Modifier.weight(1f)
            )
            BentoStatTile(
                title = "গড় দর",
                value = BengaliFormatter.formatTaka(state.weightedAverageRate),
                icon = Icons.Rounded.Payments,
                tint = CrateAmber,
                bg = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BentoStatTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, BentoBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = SlateInk,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualityGradeCard(
    grade: GradeEntry,
    onQuickAddCarton: () -> Unit,
    onEditCarton: (Int, Double) -> Unit,
    onDeleteCarton: (Int) -> Unit,
    onRateChange: (Double) -> Unit,
    onEditGrade: () -> Unit,
    onDeleteGrade: () -> Unit
) {
    var rateText by remember(grade.gradeId) {
        mutableStateOf(if (grade.ratePerKg > 0.0) grade.ratePerKg.toBigDecimal().stripTrailingZeros().toPlainString() else "")
    }
    LaunchedEffect(grade.ratePerKg) {
        val currentParsed = BengaliFormatter.parseNumericInput(rateText) ?: 0.0
        if (kotlin.math.abs(currentParsed - grade.ratePerKg) > 0.01) {
            rateText = if (grade.ratePerKg > 0.0) grade.ratePerKg.toBigDecimal().stripTrailingZeros().toPlainString() else ""
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BentoBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Grade Pill + Edit & Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StrawberryTint
                    ) {
                        Text(
                            text = grade.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = StrawberryDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "${BengaliFormatter.formatInt(grade.totalCartons)} কার্টুন",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditGrade,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_grade_${grade.gradeId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "${grade.name} সম্পাদনা করুন",
                            tint = SlateMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteGrade,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_grade_${grade.gradeId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "${grade.name} মুছে ফেলুন",
                            tint = StrawberryRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Selling Rate & + Add Carton Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { txt ->
                        rateText = txt
                        val parsed = BengaliFormatter.parseNumericInput(txt) ?: 0.0
                        onRateChange(parsed)
                    },
                    label = { Text("বিক্রয় দর (৳/কেজি)") },
                    placeholder = { Text("০") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rate_input_${grade.gradeId}")
                )

                Button(
                    onClick = onQuickAddCarton,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StrawberryRed,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("add_carton_button_${grade.gradeId}")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কার্টুন যোগ", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }

            // Individual Carton Weight Chips with Inline Edit & Delete
            if (grade.cartonWeights.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    grade.cartonWeights.forEachIndexed { index, weight ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSoftBg,
                            border = BorderStroke(1.dp, EmeraldTint),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { onEditCarton(index, weight) }
                                        .padding(vertical = 6.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "#${BengaliFormatter.formatInt(index + 1)}: ${BengaliFormatter.formatNumber(weight)} কেজি",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SlateInk
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "ওজন পরিবর্তন করুন",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCarton(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "কার্টুন মুছুন",
                                        tint = StrawberryRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "এখনো কোনো কার্টুন যোগ করা হয়নি — '+ কার্টুন যোগ' বাটনে ট্যাপ করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted
                )
            }

            HorizontalDivider(color = BentoBorder)

            // Grade Footer Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মোট নিট ওজন: ${BengaliFormatter.formatKg(grade.totalWeightKg)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateInk,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = BengaliFormatter.formatTaka(grade.subtotalSales),
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeductionsBentoCard(
    state: CalculatorState,
    onUpdateDeductions: (Double, Double, Double) -> Unit
) {
    var commText by remember {
        mutableStateOf(state.commissionPercent.toBigDecimal().stripTrailingZeros().toPlainString())
    }
    var kuliText by remember {
        mutableStateOf(if (state.kuliExpense > 0.0) state.kuliExpense.toBigDecimal().stripTrailingZeros().toPlainString() else "")
    }
    var otherText by remember {
        mutableStateOf(if (state.otherExpense > 0.0) state.otherExpense.toBigDecimal().stripTrailingZeros().toPlainString() else "")
    }

    LaunchedEffect(state.commissionPercent, state.kuliExpense, state.otherExpense) {
        val c = BengaliFormatter.parseNumericInput(commText) ?: 0.0
        if (kotlin.math.abs(c - state.commissionPercent) > 0.01) {
            commText = state.commissionPercent.toBigDecimal().stripTrailingZeros().toPlainString()
        }
        val k = BengaliFormatter.parseNumericInput(kuliText) ?: 0.0
        if (kotlin.math.abs(k - state.kuliExpense) > 0.01) {
            kuliText = if (state.kuliExpense > 0.0) state.kuliExpense.toBigDecimal().stripTrailingZeros().toPlainString() else ""
        }
        val o = BengaliFormatter.parseNumericInput(otherText) ?: 0.0
        if (kotlin.math.abs(o - state.otherExpense) > 0.01) {
            otherText = if (state.otherExpense > 0.0) state.otherExpense.toBigDecimal().stripTrailingZeros().toPlainString() else ""
        }
    }

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
            Text(
                text = "কর্তন ও খরচ (Deductions)",
                style = MaterialTheme.typography.titleMedium,
                color = SlateInk
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commText,
                    onValueChange = { txt ->
                        commText = txt
                        onUpdateDeductions(
                            BengaliFormatter.parseNumericInput(txt) ?: 0.0,
                            state.kuliExpense,
                            state.otherExpense
                        )
                    },
                    label = { Text("আড়ত কমিশন (%)") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StrawberrySoftBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("কমিশন টাকা", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                        Text(
                            text = BengaliFormatter.formatTaka(state.commissionAmount),
                            style = MaterialTheme.typography.titleSmall,
                            color = StrawberryRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = kuliText,
                    onValueChange = { txt ->
                        kuliText = txt
                        onUpdateDeductions(
                            state.commissionPercent,
                            BengaliFormatter.parseNumericInput(txt) ?: 0.0,
                            state.otherExpense
                        )
                    },
                    label = { Text("কুলি খরচ (৳)") },
                    placeholder = { Text("০") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = otherText,
                    onValueChange = { txt ->
                        otherText = txt
                        onUpdateDeductions(
                            state.commissionPercent,
                            state.kuliExpense,
                            BengaliFormatter.parseNumericInput(txt) ?: 0.0
                        )
                    },
                    label = { Text("অন্যান্য খরচ (৳)") },
                    placeholder = { Text("০") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChalanMetadataCard(
    state: CalculatorState,
    onUpdateMetadata: (String, String, String, String, String) -> Unit
) {
    val paymentOptions = listOf("নগদ (Cash)", "বিকাশ (bKash)", "নগদ (Nagad)", "ব্যাংক (Bank)", "বাকি (Due)")

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = EmeraldGreen)
                Text(
                    text = "চালান নোট ও পেমেন্ট তথ্য",
                    style = MaterialTheme.typography.titleMedium,
                    color = SlateInk
                )
            }

            OutlinedTextField(
                value = state.aratName,
                onValueChange = {
                    onUpdateMetadata(it, state.supplierName, state.chalanNote, state.paymentMethod, state.signatureText)
                },
                label = { Text("আড়তের নাম") },
                singleLine = true,
                colors = appTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.supplierName,
                onValueChange = {
                    onUpdateMetadata(state.aratName, it, state.chalanNote, state.paymentMethod, state.signatureText)
                },
                label = { Text("মালিক / মহাজনের নাম") },
                placeholder = { Text("যেমন: হাজী রহিম উদ্দিন") },
                singleLine = true,
                colors = appTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.chalanNote,
                onValueChange = {
                    onUpdateMetadata(state.aratName, state.supplierName, it, state.paymentMethod, state.signatureText)
                },
                label = { Text("চালান নোট (ট্রাক নং, ড্রাইভার ফোন, ফলের অবস্থা)") },
                placeholder = { Text("যেমন: ঢাকা মেট্রো-ট ১১-২২৩৩, ০১৭..., ফল একদম ফ্রেশ") },
                minLines = 2,
                maxLines = 4,
                colors = appTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "পেমেন্ট মাধ্যম",
                style = MaterialTheme.typography.labelLarge,
                color = SlateInk
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                paymentOptions.forEach { method ->
                    FilterChip(
                        selected = state.paymentMethod == method,
                        onClick = {
                            onUpdateMetadata(state.aratName, state.supplierName, state.chalanNote, method, state.signatureText)
                        },
                        label = { Text(method) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = SlateMuted,
                            selectedContainerColor = EmeraldTint,
                            selectedLabelColor = EmeraldDark
                        )
                    )
                }
            }

            OutlinedTextField(
                value = state.signatureText,
                onValueChange = {
                    onUpdateMetadata(state.aratName, state.supplierName, state.chalanNote, state.paymentMethod, it)
                },
                label = { Text("স্বাক্ষর / সুপারভাইজার নাম") },
                placeholder = { Text("আপনার স্বাক্ষর লিখুন") },
                singleLine = true,
                colors = appTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartonWeightDialog(
    target: CartonEditTarget,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var weightText by remember {
        mutableStateOf(
            if (target.currentWeight > 0.0) target.currentWeight.toBigDecimal().stripTrailingZeros().toPlainString()
            else ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val presets = listOf(1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 5.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = SlateInk,
        textContentColor = SlateMuted,
        title = {
            Text(
                text = if (target.index == null) "${target.gradeName}: নতুন কার্টুন ওজন"
                else "${target.gradeName}: কার্টুন ওজন পরিবর্তন",
                color = SlateInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "কার্টুনের গায়ে লেখা নিট ওজন (কেজি) ট্যাপ করুন বা লিখুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        Surface(
                            onClick = { onConfirm(preset) },
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSoftBg,
                            border = BorderStroke(1.dp, EmeraldTint),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "${BengaliFormatter.formatNumber(preset)} কেজি",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        error = null
                    },
                    label = { Text("কাস্টম নিট ওজন (কেজি)") },
                    placeholder = { Text("যেমন: ২.৭৫") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(it, color = StrawberryRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = BengaliFormatter.parseNumericInput(weightText)
                    if (parsed == null || parsed <= 0.0) {
                        error = "সঠিক নিট ওজন লিখুন (যেমন: ২.৫)"
                    } else {
                        onConfirm(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StrawberryRed,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (target.index == null) "যোগ করুন" else "আপডেট", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = SlateInk)
            }
        }
    )
}

@Composable
private fun GradeDialog(
    initialName: String,
    initialRate: Double,
    title: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var rateText by remember {
        mutableStateOf(if (initialRate > 0.0) initialRate.toBigDecimal().stripTrailingZeros().toPlainString() else "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = SlateInk,
        textContentColor = SlateMuted,
        title = { Text(title, color = SlateInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("কোয়ালিটি নাম") },
                    placeholder = { Text("যেমন: Super Jumbo") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("বিক্রয় দর (৳/কেজি)") },
                    placeholder = { Text("০") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = StrawberryRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        error = "কোয়ালিটির নাম লিখুন।"
                    } else {
                        val rate = BengaliFormatter.parseNumericInput(rateText) ?: 0.0
                        onConfirm(name.trim(), rate)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StrawberryRed,
                    contentColor = Color.White
                )
            ) {
                Text(confirmText, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল", color = SlateInk) }
        }
    )
}
