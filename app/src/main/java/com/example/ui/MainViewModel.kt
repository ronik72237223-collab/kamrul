package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AratRepository
import com.example.data.model.AppConfig
import com.example.data.model.AratLedger
import com.example.data.model.CalculatorState
import com.example.data.model.DailyExpense
import com.example.data.model.GradeEntry
import com.example.data.model.SUPER_ADMIN_UID
import com.example.data.model.TrashItem
import com.example.data.model.TrashItemType
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.util.BengaliFormatter
import com.example.util.ExcelExporter
import com.example.util.ExportResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface MainUiEvent {
    data class ShowMessage(val message: String) : MainUiEvent
    data class ExcelExported(val result: ExportResult) : MainUiEvent
    data object SwitchToCalculatorTab : MainUiEvent
}

class MainViewModel(
    private val repository: AratRepository,
    val currentUserId: String
) : ViewModel() {

    val isSuperAdmin: Boolean = currentUserId == SUPER_ADMIN_UID

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppConfig()
        )

    val ledgersState: StateFlow<UiState<List<AratLedger>>> = repository.observeLedgers(currentUserId)
        .map<List<AratLedger>, UiState<List<AratLedger>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing ledgers", root)
            emit(UiState.Error(root.message ?: "হিসাব খাতা লোড করতে সমস্যা হয়েছে।"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val expensesState: StateFlow<UiState<List<DailyExpense>>> = repository.observeExpenses(currentUserId)
        .map<List<DailyExpense>, UiState<List<DailyExpense>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing expenses", root)
            emit(UiState.Error(root.message ?: "হাতখরচ লোড করতে সমস্যা হয়েছে।"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val trashItemsState: StateFlow<List<TrashItem>> = repository.observeTrashItems(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing trash items", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val adminUsersState: StateFlow<List<UserProfile>> = repository.observeAllUsersForAdmin()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing admin users", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _calculator = MutableStateFlow(buildDefaultCalculatorState(AppConfig()))
    val calculator: StateFlow<CalculatorState> = _calculator.asStateFlow()

    private val _undoStack = MutableStateFlow<List<CalculatorState>>(emptyList())
    private val _redoStack = MutableStateFlow<List<CalculatorState>>(emptyList())
    val canUndo: StateFlow<Boolean> = _undoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)
    val canRedo: StateFlow<Boolean> = _redoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var draftSaveJob: Job? = null
    private var initialDraftLoaded = false

    init {
        viewModelScope.launch {
            val savedDraft = repository.loadTodayDraft(currentUserId)
            val cfg = appConfig.value
            if (savedDraft != null && savedDraft.grades.isNotEmpty()) {
                _calculator.value = savedDraft
            } else {
                _calculator.value = buildDefaultCalculatorState(cfg)
            }
            initialDraftLoaded = true
        }

        // Sync Admin default grades/aratName when calculator has no entered weights yet
        viewModelScope.launch {
            appConfig.collect { cfg ->
                val cur = _calculator.value
                if (initialDraftLoaded && cur.totalCartons == 0L && cur.editingLedgerId == null) {
                    val hasAnyRate = cur.grades.any { it.ratePerKg > 0.0 }
                    if (!hasAnyRate) {
                        _calculator.value = cur.copy(
                            aratName = cfg.aratName,
                            commissionPercent = cfg.defaultCommissionPercent,
                            kuliExpense = if (cur.kuliExpense == 0.0) cfg.defaultKuliRate else cur.kuliExpense,
                            grades = cfg.qualityGrades.mapIndexed { idx, name ->
                                val existing = cur.grades.getOrNull(idx)
                                GradeEntry(
                                    gradeId = existing?.gradeId?.ifBlank { "grade_$idx" } ?: "grade_$idx",
                                    name = name,
                                    ratePerKg = existing?.ratePerKg ?: 0.0,
                                    cartonWeights = existing?.cartonWeights ?: emptyList()
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun buildDefaultCalculatorState(cfg: AppConfig): CalculatorState {
        return CalculatorState(
            dateText = BengaliFormatter.currentDateIso(),
            displayDate = BengaliFormatter.currentDateBengali(),
            aratName = cfg.aratName,
            supplierName = "",
            grades = cfg.qualityGrades.mapIndexed { index, name ->
                GradeEntry(
                    gradeId = "grade_${index}_${name.hashCode()}",
                    name = name,
                    ratePerKg = 0.0,
                    cartonWeights = emptyList()
                )
            },
            commissionPercent = cfg.defaultCommissionPercent,
            kuliExpense = cfg.defaultKuliRate,
            otherExpense = 0.0,
            chalanNote = "",
            paymentMethod = "নগদ (Cash)",
            signatureText = ""
        )
    }

    private fun mutateCalculator(transform: (CalculatorState) -> CalculatorState) {
        val current = _calculator.value
        val updated = transform(current)
        if (updated == current) return

        _undoStack.update { (it + current).takeLast(MAX_UNDO_HISTORY) }
        _redoStack.value = emptyList()
        _calculator.value = updated
        scheduleDraftSync(updated)
    }

    private fun scheduleDraftSync(state: CalculatorState) {
        if (state.editingLedgerId != null) return
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(550L)
            repository.saveTodayDraft(currentUserId, state)
        }
    }

    fun undo() {
        val stack = _undoStack.value
        if (stack.isEmpty()) return
        val previous = stack.last()
        _undoStack.value = stack.dropLast(1)
        _redoStack.update { (it + _calculator.value).takeLast(MAX_UNDO_HISTORY) }
        _calculator.value = previous
        scheduleDraftSync(previous)
        viewModelScope.launch {
            _events.emit(MainUiEvent.ShowMessage("পূর্বের অবস্থায় ফিরে যাওয়া হয়েছে (Undo)"))
        }
    }

    fun redo() {
        val stack = _redoStack.value
        if (stack.isEmpty()) return
        val next = stack.last()
        _redoStack.value = stack.dropLast(1)
        _undoStack.update { (it + _calculator.value).takeLast(MAX_UNDO_HISTORY) }
        _calculator.value = next
        scheduleDraftSync(next)
        viewModelScope.launch {
            _events.emit(MainUiEvent.ShowMessage("পুনরায় প্রয়োগ করা হয়েছে (Redo)"))
        }
    }

    // Carton & Quality Grade Actions
    fun addCartonWeight(gradeId: String, weightKg: Double) {
        if (weightKg <= 0.0 || weightKg > 10000.0) return
        mutateCalculator { state ->
            state.copy(
                grades = state.grades.map { grade ->
                    if (grade.gradeId == gradeId) {
                        grade.copy(cartonWeights = grade.cartonWeights + weightKg)
                    } else grade
                }
            )
        }
    }

    fun editCartonWeight(gradeId: String, cartonIndex: Int, newWeightKg: Double) {
        if (newWeightKg <= 0.0 || newWeightKg > 10000.0) return
        mutateCalculator { state ->
            state.copy(
                grades = state.grades.map { grade ->
                    if (grade.gradeId == gradeId && cartonIndex in grade.cartonWeights.indices) {
                        val mutable = grade.cartonWeights.toMutableList()
                        mutable[cartonIndex] = newWeightKg
                        grade.copy(cartonWeights = mutable)
                    } else grade
                }
            )
        }
    }

    fun deleteCartonWeight(gradeId: String, cartonIndex: Int) {
        val grade = _calculator.value.grades.find { it.gradeId == gradeId } ?: return
        val weight = grade.cartonWeights.getOrNull(cartonIndex) ?: return

        mutateCalculator { state ->
            state.copy(
                grades = state.grades.map { g ->
                    if (g.gradeId == gradeId && cartonIndex in g.cartonWeights.indices) {
                        val mutable = g.cartonWeights.toMutableList()
                        mutable.removeAt(cartonIndex)
                        g.copy(cartonWeights = mutable)
                    } else g
                }
            )
        }

        viewModelScope.launch {
            repository.addTrashItem(
                userId = currentUserId,
                itemType = TrashItemType.CARTON.name,
                gradeName = grade.name,
                weightKg = weight,
                ratePerKg = grade.ratePerKg,
                cartonWeights = emptyList()
            )
            _events.emit(MainUiEvent.ShowMessage("${grade.name}-এর ${BengaliFormatter.formatKg(weight)} রিসাইকেল বিনে পাঠানো হয়েছে।"))
        }
    }

    fun updateGradeRate(gradeId: String, ratePerKg: Double) {
        val clean = ratePerKg.coerceIn(0.0, 1000000.0)
        mutateCalculator { state ->
            state.copy(
                grades = state.grades.map { g ->
                    if (g.gradeId == gradeId) g.copy(ratePerKg = clean) else g
                }
            )
        }
    }

    fun addCustomGrade(name: String, ratePerKg: Double) {
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) return
        val newGrade = GradeEntry(
            gradeId = "custom_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            ratePerKg = ratePerKg.coerceIn(0.0, 1000000.0),
            cartonWeights = emptyList()
        )
        mutateCalculator { state ->
            state.copy(grades = state.grades + newGrade)
        }
    }

    fun renameGrade(gradeId: String, newName: String, newRate: Double) {
        val clean = newName.trim().take(60)
        if (clean.isBlank()) return
        mutateCalculator { state ->
            state.copy(
                grades = state.grades.map { g ->
                    if (g.gradeId == gradeId) {
                        g.copy(name = clean, ratePerKg = newRate.coerceIn(0.0, 1000000.0))
                    } else g
                }
            )
        }
    }

    fun deleteGrade(gradeId: String) {
        val grade = _calculator.value.grades.find { it.gradeId == gradeId } ?: return
        mutateCalculator { state ->
            state.copy(grades = state.grades.filterNot { it.gradeId == gradeId })
        }
        viewModelScope.launch {
            repository.addTrashItem(
                userId = currentUserId,
                itemType = TrashItemType.GRADE.name,
                gradeName = grade.name,
                weightKg = grade.totalWeightKg,
                ratePerKg = grade.ratePerKg,
                cartonWeights = grade.cartonWeights
            )
            _events.emit(MainUiEvent.ShowMessage("\"${grade.name}\" কোয়ালিটি রিসাইকেল বিনে পাঠানো হয়েছে।"))
        }
    }

    fun updateDeductions(commissionPercent: Double, kuliExpense: Double, otherExpense: Double) {
        mutateCalculator { state ->
            state.copy(
                commissionPercent = commissionPercent.coerceIn(0.0, 100.0),
                kuliExpense = kuliExpense.coerceIn(0.0, 10000000.0),
                otherExpense = otherExpense.coerceIn(0.0, 10000000.0)
            )
        }
    }

    fun updateMetadata(
        aratName: String,
        supplierName: String,
        chalanNote: String,
        paymentMethod: String,
        signatureText: String
    ) {
        mutateCalculator { state ->
            state.copy(
                aratName = aratName.take(100),
                supplierName = supplierName.take(100),
                chalanNote = chalanNote.take(500),
                paymentMethod = paymentMethod.take(50),
                signatureText = signatureText.take(100)
            )
        }
    }

    fun saveCurrentLedgerToKhata(resetAfterSave: Boolean) {
        val state = _calculator.value
        if (state.totalCartons == 0L) {
            viewModelScope.launch {
                _events.emit(MainUiEvent.ShowMessage("সংরক্ষণ করার আগে কমপক্ষে ১টি কার্টুন যোগ করুন।"))
            }
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveLedger(currentUserId, state)
            _isSaving.value = false
            result.onSuccess {
                if (resetAfterSave) {
                    val fresh = buildDefaultCalculatorState(appConfig.value)
                    _calculator.value = fresh
                    _undoStack.value = emptyList()
                    _redoStack.value = emptyList()
                    repository.saveTodayDraft(currentUserId, fresh)
                    _events.emit(MainUiEvent.ShowMessage("হিসাব খাতায় সংরক্ষণ করা হয়েছে ও নতুন হিসাব প্রস্তুত!"))
                } else {
                    _events.emit(MainUiEvent.ShowMessage("হিসাব খাতায় সফলভাবে সংরক্ষিত হয়েছে!"))
                }
            }.onFailure { err ->
                _events.emit(MainUiEvent.ShowMessage("সংরক্ষণ ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"))
            }
        }
    }

    fun startNewCalculation() {
        val fresh = buildDefaultCalculatorState(appConfig.value)
        mutateCalculator { fresh }
        viewModelScope.launch {
            _events.emit(MainUiEvent.ShowMessage("নতুন দিনের হিসাব শুরু হয়েছে।"))
        }
    }

    fun loadLedgerForEditing(ledger: AratLedger) {
        val loaded = CalculatorState(
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
            signatureText = ledger.signatureText,
            editingLedgerId = ledger.id
        )
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        _calculator.value = loaded
        viewModelScope.launch {
            _events.emit(MainUiEvent.SwitchToCalculatorTab)
            _events.emit(MainUiEvent.ShowMessage("খাতার হিসাব সম্পাদনার জন্য খোলা হয়েছে।"))
        }
    }

    // Daily Expenses (হাতখরচ)
    fun saveExpense(expenseId: String?, category: String, description: String, amount: Double) {
        if (amount <= 0.0) {
            viewModelScope.launch {
                _events.emit(MainUiEvent.ShowMessage("সঠিক টাকার পরিমাণ লিখুন।"))
            }
            return
        }
        viewModelScope.launch {
            val res = repository.upsertExpense(
                userId = currentUserId,
                expenseId = expenseId,
                dateText = BengaliFormatter.currentDateIso(),
                category = category,
                description = description,
                amount = amount
            )
            res.onSuccess {
                _events.emit(MainUiEvent.ShowMessage("হাতখরচ সংরক্ষণ করা হয়েছে।"))
            }.onFailure { err ->
                _events.emit(MainUiEvent.ShowMessage("খরচ সেভ করতে সমস্যা: ${err.localizedMessage ?: "ত্রুটি"}"))
            }
        }
    }

    fun softDeleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.setExpenseDeleted(currentUserId, expenseId, true)
                .onSuccess { _events.emit(MainUiEvent.ShowMessage("খরচটি রিসাইকেল বিনে পাঠানো হয়েছে।")) }
        }
    }

    fun restoreExpense(expenseId: String) {
        viewModelScope.launch {
            repository.setExpenseDeleted(currentUserId, expenseId, false)
                .onSuccess { _events.emit(MainUiEvent.ShowMessage("খরচটি ফিরিয়ে আনা হয়েছে!")) }
        }
    }

    fun deleteExpensePermanently(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpensePermanently(currentUserId, expenseId)
                .onSuccess { _events.emit(MainUiEvent.ShowMessage("খরচটি স্থায়ীভাবে মুছে ফেলা হয়েছে।")) }
        }
    }

    // Ledgers Recycle Bin Actions
    fun softDeleteLedger(ledgerId: String) {
        viewModelScope.launch {
            repository.setLedgerDeleted(currentUserId, ledgerId, true)
                .onSuccess { _events.emit(MainUiEvent.ShowMessage("হিসাবটি রিসাইকেল বিনে পাঠানো হয়েছে।")) }
        }
    }

    fun restoreLedger(ledgerId: String) {
        viewModelScope.launch {
            repository.setLedgerDeleted(currentUserId, ledgerId, false)
                .onSuccess { _events.emit(MainUiEvent.ShowMessage("হিসাবটি খাতায় ফিরিয়ে আনা হয়েছে!")) }
        }
    }

    fun deleteLedgerPermanently(ledgerId: String) {
        viewModelScope.launch {
            repository.deleteLedgerPermanently(currentUserId, ledgerId)
                .onSuccess { _events.emit(MainUiEvent.ShowMessage("হিসাবটি স্থায়ীভাবে মুছে ফেলা হয়েছে।")) }
        }
    }

    // Trash Items (Deleted Cartons / Grades) Recovery
    fun restoreTrashItem(item: TrashItem) {
        mutateCalculator { state ->
            if (item.itemType == TrashItemType.GRADE.name) {
                val restoredGrade = GradeEntry(
                    gradeId = "restored_${UUID.randomUUID().toString().take(8)}",
                    name = item.gradeName,
                    ratePerKg = item.ratePerKg,
                    cartonWeights = item.cartonWeights
                )
                state.copy(grades = state.grades + restoredGrade)
            } else {
                val existingGrade = state.grades.find { it.name == item.gradeName }
                if (existingGrade != null) {
                    state.copy(
                        grades = state.grades.map { g ->
                            if (g.gradeId == existingGrade.gradeId) {
                                g.copy(cartonWeights = g.cartonWeights + item.weightKg)
                            } else g
                        }
                    )
                } else {
                    val newGrade = GradeEntry(
                        gradeId = "restored_${UUID.randomUUID().toString().take(8)}",
                        name = item.gradeName,
                        ratePerKg = item.ratePerKg,
                        cartonWeights = listOf(item.weightKg)
                    )
                    state.copy(grades = state.grades + newGrade)
                }
            }
        }
        viewModelScope.launch {
            repository.deleteTrashItemPermanently(currentUserId, item.id)
            _events.emit(MainUiEvent.ShowMessage("\"${item.gradeName}\" সফলভাবে ফিরিয়ে আনা হয়েছে!"))
        }
    }

    fun deleteTrashItemPermanently(trashId: String) {
        viewModelScope.launch {
            repository.deleteTrashItemPermanently(currentUserId, trashId)
        }
    }

    fun emptyRecycleBin(
        deletedLedgers: List<AratLedger>,
        deletedExpenses: List<DailyExpense>,
        trashItems: List<TrashItem>
    ) {
        viewModelScope.launch {
            deletedLedgers.forEach { repository.deleteLedgerPermanently(currentUserId, it.id) }
            deletedExpenses.forEach { repository.deleteExpensePermanently(currentUserId, it.id) }
            trashItems.forEach { repository.deleteTrashItemPermanently(currentUserId, it.id) }
            _events.emit(MainUiEvent.ShowMessage("রিসাইকেল বিন সম্পূর্ণ খালি করা হয়েছে।"))
        }
    }

    // Excel Export
    fun exportLedgersToExcel(context: Context, ledgers: List<AratLedger>) {
        if (ledgers.isEmpty()) {
            viewModelScope.launch {
                _events.emit(MainUiEvent.ShowMessage("এক্সেল ডাউনলোড করার আগে খাতায় অন্তত ১টি হিসাব সেভ করুন।"))
            }
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = ExcelExporter.exportLedgersToXlsx(context.applicationContext, ledgers)
            _isSaving.value = false
            res.onSuccess { exported ->
                _events.emit(MainUiEvent.ExcelExported(exported))
            }.onFailure { err ->
                _events.emit(MainUiEvent.ShowMessage("এক্সেল তৈরি করতে সমস্যা: ${err.localizedMessage ?: "ত্রুটি"}"))
            }
        }
    }

    // Super Admin Actions
    fun updateAdminConfig(config: AppConfig) {
        if (!isSuperAdmin) return
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateAppConfig(config)
            _isSaving.value = false
            res.onSuccess {
                _events.emit(MainUiEvent.ShowMessage("অ্যাডমিন সেটিংস লাইভ আপডেট হয়েছে!"))
            }.onFailure { err ->
                _events.emit(MainUiEvent.ShowMessage("আপডেট ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"))
            }
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val MAX_UNDO_HISTORY = 50
    }
}
