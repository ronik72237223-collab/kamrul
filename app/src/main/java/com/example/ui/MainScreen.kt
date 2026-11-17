package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UiState
import com.example.ui.admin.AdminScreen
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.expenses.ExpensesScreen
import com.example.ui.ledger.LedgerScreen
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.StrawberryTint
import com.example.ui.trash.RecycleBinSheet
import com.example.util.BengaliFormatter
import com.example.util.ExcelExporter

enum class AppTab(val label: String, val icon: ImageVector) {
    CALCULATOR("আজকের হিসাব", Icons.Rounded.Calculate),
    EXPENSES("হাতখরচ", Icons.Rounded.AccountBalanceWallet),
    LEDGER("খাতা ও রিপোর্ট", Icons.Rounded.Book),
    ADMIN("অ্যাডমিন", Icons.Rounded.AdminPanelSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val calculatorState by viewModel.calculator.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val ledgersState by viewModel.ledgersState.collectAsStateWithLifecycle()
    val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
    val trashItems by viewModel.trashItemsState.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsersState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.CALCULATOR) }
    var showRecycleBin by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val deletedLedgers = remember(ledgersState) {
        (ledgersState as? UiState.Success)?.data?.filter { it.deleted } ?: emptyList()
    }
    val deletedExpenses = remember(expensesState) {
        (expensesState as? UiState.Success)?.data?.filter { it.deleted } ?: emptyList()
    }
    val totalTrashCount = deletedLedgers.size + deletedExpenses.size + trashItems.size

    val visibleTabs = remember(viewModel.isSuperAdmin) {
        if (viewModel.isSuperAdmin) {
            listOf(AppTab.CALCULATOR, AppTab.EXPENSES, AppTab.LEDGER, AppTab.ADMIN)
        } else {
            listOf(AppTab.CALCULATOR, AppTab.EXPENSES, AppTab.LEDGER)
        }
    }

    // Back press on secondary tabs returns to Calculator tab cleanly
    BackHandler(enabled = selectedTab != AppTab.CALCULATOR) {
        selectedTab = AppTab.CALCULATOR
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MainUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MainUiEvent.SwitchToCalculatorTab -> {
                    selectedTab = AppTab.CALCULATOR
                }
                is MainUiEvent.ExcelExported -> {
                    val res = snackbarHostState.showSnackbar(
                        message = "এক্সেল সেভ হয়েছে: ${event.result.displayLocation}",
                        actionLabel = "শেয়ার / ওপেন",
                        duration = SnackbarDuration.Long
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        ExcelExporter.openOrShareExcel(context, event.result.uri)
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = BentoBackground,
            contentColor = SlateInk,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = SlateInk,
                        actionIconContentColor = SlateInk,
                        navigationIconContentColor = SlateInk
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_brand_logo),
                                contentDescription = "লোগো",
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = appConfig.appName.ifBlank { "স্ট্রবেরি আড়ত প্রো" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SlateInk,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = appConfig.aratName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // Prominent Undo / Redo in Top Bar
                        IconButton(
                            onClick = viewModel::undo,
                            enabled = canUndo,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = "পূর্বাবস্থায় ফিরুন (Undo)",
                                tint = if (canUndo) StrawberryRed else SlateMuted.copy(alpha = 0.35f)
                            )
                        }

                        IconButton(
                            onClick = viewModel::redo,
                            enabled = canRedo,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Redo,
                                contentDescription = "পুনরায় করুন (Redo)",
                                tint = if (canRedo) EmeraldGreen else SlateMuted.copy(alpha = 0.35f)
                            )
                        }

                        // Recycle Bin Icon with Badge
                        IconButton(
                            onClick = { showRecycleBin = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("recycle_bin_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalTrashCount > 0) {
                                        Badge(
                                            containerColor = StrawberryRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(BengaliFormatter.formatInt(totalTrashCount))
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "রিসাইকেল বিন",
                                    tint = SlateInk
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "লগআউট",
                                tint = SlateInk
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = Color.White,
                        contentColor = SlateInk
                    ) {
                        visibleTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StrawberryRed,
                                    selectedTextColor = StrawberryRed,
                                    unselectedIconColor = SlateMuted,
                                    unselectedTextColor = SlateMuted,
                                    indicatorColor = StrawberryTint
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = Color.White,
                        contentColor = SlateInk,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        visibleTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = StrawberryRed,
                                    selectedTextColor = StrawberryRed,
                                    unselectedIconColor = SlateMuted,
                                    unselectedTextColor = SlateMuted,
                                    indicatorColor = StrawberryTint
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp)) {
                        when (selectedTab) {
                            AppTab.CALCULATOR -> CalculatorScreen(
                                state = calculatorState,
                                appConfig = appConfig,
                                isSaving = isSaving,
                                onAddCarton = viewModel::addCartonWeight,
                                onEditCarton = viewModel::editCartonWeight,
                                onDeleteCarton = viewModel::deleteCartonWeight,
                                onUpdateGradeRate = viewModel::updateGradeRate,
                                onAddCustomGrade = viewModel::addCustomGrade,
                                onRenameGrade = viewModel::renameGrade,
                                onDeleteGrade = viewModel::deleteGrade,
                                onUpdateDeductions = viewModel::updateDeductions,
                                onUpdateMetadata = viewModel::updateMetadata,
                                onSaveLedger = viewModel::saveCurrentLedgerToKhata,
                                onStartNew = viewModel::startNewCalculation
                            )
                            AppTab.EXPENSES -> ExpensesScreen(
                                expensesState = expensesState,
                                onSaveExpense = viewModel::saveExpense,
                                onDeleteExpense = viewModel::softDeleteExpense
                            )
                            AppTab.LEDGER -> LedgerScreen(
                                ledgersState = ledgersState,
                                isExporting = isSaving,
                                onExportExcel = { list ->
                                    viewModel.exportLedgersToExcel(context, list)
                                },
                                onEditLedger = viewModel::loadLedgerForEditing,
                                onDeleteLedger = viewModel::softDeleteLedger
                            )
                            AppTab.ADMIN -> {
                                if (viewModel.isSuperAdmin) {
                                    AdminScreen(
                                        appConfig = appConfig,
                                        users = adminUsers,
                                        isSaving = isSaving,
                                        onSaveConfig = viewModel::updateAdminConfig
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecycleBin) {
        RecycleBinSheet(
            deletedLedgers = deletedLedgers,
            deletedExpenses = deletedExpenses,
            trashItems = trashItems,
            onRestoreLedger = viewModel::restoreLedger,
            onDeleteLedgerPermanently = viewModel::deleteLedgerPermanently,
            onRestoreExpense = viewModel::restoreExpense,
            onDeleteExpensePermanently = viewModel::deleteExpensePermanently,
            onRestoreTrashItem = viewModel::restoreTrashItem,
            onDeleteTrashItemPermanently = viewModel::deleteTrashItemPermanently,
            onEmptyBin = {
                viewModel.emptyRecycleBin(deletedLedgers, deletedExpenses, trashItems)
            },
            onDismiss = { showRecycleBin = false }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = Color.White,
            titleContentColor = SlateInk,
            textContentColor = SlateMuted,
            title = { Text("লগআউট করবেন?", color = SlateInk) },
            text = { Text("আপনার সব হিসাব নিরাপদে সংরক্ষিত আছে। লগআউট করতে চান?", color = SlateMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text("হ্যাঁ, লগআউট", color = StrawberryRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("থাকুন", color = SlateInk)
                }
            }
        )
    }
}
