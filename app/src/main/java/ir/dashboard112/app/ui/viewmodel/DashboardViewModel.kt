package ir.dashboard112.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.dashboard112.app.data.local.entity.FinanceTransactionEntity
import ir.dashboard112.app.data.local.entity.OfficeTaskEntity
import ir.dashboard112.app.data.local.entity.ReminderEntity
import ir.dashboard112.app.data.local.entity.RepeatInterval
import ir.dashboard112.app.data.local.entity.SleepRecordEntity
import ir.dashboard112.app.data.local.entity.TaskEntity
import ir.dashboard112.app.data.local.entity.TaskPriority
import ir.dashboard112.app.data.local.entity.TransactionType
import ir.dashboard112.app.data.local.entity.VehicleServiceEntity
import ir.dashboard112.app.data.local.entity.VehicleUrgency
import ir.dashboard112.app.data.local.entity.WorkoutEntity
import ir.dashboard112.app.data.preferences.ThemeModePreference
import ir.dashboard112.app.data.preferences.UserPreferencesRepository
import ir.dashboard112.app.data.preferences.UserPreferencesState
import ir.dashboard112.app.data.repository.DashboardRepository
import ir.dashboard112.app.domain.util.PersianDateUtils
import ir.dashboard112.app.domain.util.SleepCalculator
import ir.dashboard112.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = false,
    val todayIso: String = LocalDate.now().toString(),
    val selectedJalaliMonth: String = PersianDateUtils.isoDateToJalaliYearMonth(LocalDate.now().toString()),
    val searchQuery: String = "",
    val selectedPriorityFilter: TaskPriority? = null,
    val sortByPriorityAscending: Boolean = true,
    val tasks: List<TaskEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val financeTransactions: List<FinanceTransactionEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val officeTasks: List<OfficeTaskEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val sleepRecords: List<SleepRecordEntity> = emptyList(),
    val preferences: UserPreferencesState = UserPreferencesState()
)

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_ACTIVE_ROUTE = "active_route"
        private const val KEY_SEARCH_QUERY = "search_query"
    }

    private val searchQueryFlow = MutableStateFlow(savedStateHandle[KEY_SEARCH_QUERY] ?: "")
    private val priorityFilterFlow = MutableStateFlow<TaskPriority?>(null)
    private val sortByPriorityFlow = MutableStateFlow(true)
    private val selectedMonthFlow = MutableStateFlow(
        PersianDateUtils.isoDateToJalaliYearMonth(LocalDate.now().toString())
    )

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    val activeRoute: StateFlow<String> = savedStateHandle.getStateFlow(KEY_ACTIVE_ROUTE, "dashboard")

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.tasksFlow,
        repository.workoutsFlow,
        repository.financeFlow,
        repository.vehicleFlow,
        repository.officeTasksFlow,
        repository.remindersFlow,
        repository.sleepRecordsFlow,
        preferencesRepository.preferencesFlow,
        searchQueryFlow,
        priorityFilterFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allTasks = values[0] as List<TaskEntity>
        @Suppress("UNCHECKED_CAST")
        val workouts = values[1] as List<WorkoutEntity>
        @Suppress("UNCHECKED_CAST")
        val finance = values[2] as List<FinanceTransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val vehicles = values[3] as List<VehicleServiceEntity>
        @Suppress("UNCHECKED_CAST")
        val office = values[4] as List<OfficeTaskEntity>
        @Suppress("UNCHECKED_CAST")
        val reminders = values[5] as List<ReminderEntity>
        @Suppress("UNCHECKED_CAST")
        val sleep = values[6] as List<SleepRecordEntity>
        val prefs = values[7] as UserPreferencesState
        val query = (values[8] as String).trim()
        val filter = values[9] as TaskPriority?

        val filteredTasks = allTasks.filter { task ->
            val matchesQuery = query.isEmpty() ||
                task.title.contains(query, ignoreCase = true) ||
                task.note.contains(query, ignoreCase = true)
            val matchesPriority = filter == null || task.priority == filter
            matchesQuery && matchesPriority
        }

        DashboardUiState(
            isLoading = false,
            todayIso = LocalDate.now().toString(),
            selectedJalaliMonth = selectedMonthFlow.value,
            searchQuery = query,
            selectedPriorityFilter = filter,
            sortByPriorityAscending = sortByPriorityFlow.value,
            tasks = filteredTasks,
            workouts = workouts,
            financeTransactions = finance,
            vehicleServices = vehicles,
            officeTasks = office,
            reminders = reminders,
            sleepRecords = sleep,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setActiveRoute(route: String) {
        savedStateHandle[KEY_ACTIVE_ROUTE] = route
    }

    fun updateSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        searchQueryFlow.value = query
    }

    fun updatePriorityFilter(priority: TaskPriority?) {
        priorityFilterFlow.value = priority
    }

    fun updateSelectedJalaliMonth(yearMonth: String) {
        selectedMonthFlow.value = yearMonth
    }

    fun saveTask(
        id: Long = 0,
        title: String,
        note: String,
        priority: TaskPriority,
        dateIso: String,
        isCompleted: Boolean = false
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            viewModelScope.launch { _uiEvents.emit("خطا: لطفاً عنوان کار را وارد کنید") }
            return
        }
        viewModelScope.launch {
            repository.upsertTask(
                TaskEntity(
                    id = id,
                    title = cleanTitle,
                    note = note.trim(),
                    priority = priority,
                    dateIso = dateIso,
                    isCompleted = isCompleted
                )
            )
            _uiEvents.emit("کار با موفقیت ذخیره شد")
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.upsertTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _uiEvents.emit("کار انتخاب‌شده حذف شد")
        }
    }

    fun saveWorkout(
        id: Long = 0,
        workoutType: String,
        durationMinutes: Int,
        bodyWeightKg: Double,
        personalRecordText: String,
        targetProgressPercent: Int,
        dateIso: String
    ) {
        if (workoutType.isBlank() || durationMinutes <= 0 || bodyWeightKg <= 20.0) {
            viewModelScope.launch { _uiEvents.emit("خطا: اطلاعات تمرین، مدت یا وزن نامعتبر است") }
            return
        }
        viewModelScope.launch {
            repository.upsertWorkout(
                WorkoutEntity(
                    id = id,
                    workoutType = workoutType.trim(),
                    durationMinutes = durationMinutes,
                    bodyWeightKg = bodyWeightKg,
                    personalRecordText = personalRecordText.trim(),
                    targetProgressPercent = targetProgressPercent.coerceIn(0, 100),
                    dateIso = dateIso
                )
            )
            _uiEvents.emit("جلسه تمرینی با موفقیت ثبت شد")
        }
    }

    fun saveFinanceTransaction(
        id: Long = 0,
        title: String,
        amountTomans: Long,
        type: TransactionType,
        category: String,
        dateIso: String,
        description: String
    ) {
        if (title.isBlank() || amountTomans <= 0L) {
            viewModelScope.launch { _uiEvents.emit("خطا: عنوان و مبلغ بزرگ‌تر از صفر الزامی است") }
            return
        }
        viewModelScope.launch {
            repository.upsertFinanceTransaction(
                FinanceTransactionEntity(
                    id = id,
                    title = title.trim(),
                    amountTomans = amountTomans,
                    type = type,
                    category = category.trim().ifEmpty { "عمومی" },
                    dateIso = dateIso,
                    jalaliYearMonth = PersianDateUtils.isoDateToJalaliYearMonth(dateIso),
                    description = description.trim()
                )
            )
            _uiEvents.emit("تراکنش مالی با موفقیت ثبت شد")
        }
    }

    fun saveVehicleService(
        id: Long = 0,
        vehicleName: String,
        serviceTitle: String,
        urgency: VehicleUrgency,
        currentOdometerKm: Int,
        dueOdometerKm: Int,
        costTomans: Long,
        dueDateIso: String,
        isResolved: Boolean,
        notes: String
    ) {
        if (vehicleName.isBlank() || serviceTitle.isBlank() || currentOdometerKm < 0 || costTomans < 0L) {
            viewModelScope.launch { _uiEvents.emit("خطا: اطلاعات خودرو یا کیلومتر نامعتبر است") }
            return
        }
        viewModelScope.launch {
            repository.upsertVehicleServiceWithFinanceSync(
                VehicleServiceEntity(
                    id = id,
                    vehicleName = vehicleName.trim(),
                    serviceTitle = serviceTitle.trim(),
                    urgency = urgency,
                    currentOdometerKm = currentOdometerKm,
                    dueOdometerKm = dueOdometerKm,
                    costTomans = costTomans,
                    dueDateIso = dueDateIso,
                    isResolved = isResolved,
                    notes = notes.trim()
                )
            )
            _uiEvents.emit("سرویس خودرو ذخیره و در بخش مالی همگام‌سازی شد")
        }
    }

    fun saveSleepRecord(
        id: Long = 0,
        dateIso: String,
        sleepStartHm: String,
        wakeUpHm: String,
        restAndLeisureMinutes: Int,
        qualityRating: Int,
        notes: String
    ) {
        val durationResult = SleepCalculator.calculateDurationMinutes(sleepStartHm, wakeUpHm)
        durationResult.onFailure { err ->
            viewModelScope.launch { _uiEvents.emit("خطا: ${err.message}") }
            return
        }
        if (restAndLeisureMinutes < 0) {
            viewModelScope.launch { _uiEvents.emit("خطا: زمان استراحت نمی‌تواند منفی باشد") }
            return
        }
        viewModelScope.launch {
            repository.upsertSleepRecord(
                SleepRecordEntity(
                    id = id,
                    dateIso = dateIso,
                    sleepStartHm = sleepStartHm,
                    wakeUpHm = wakeUpHm,
                    sleepDurationMinutes = durationResult.getOrThrow(),
                    restAndLeisureMinutes = restAndLeisureMinutes,
                    qualityRating = qualityRating.coerceIn(1, 5),
                    notes = notes.trim()
                )
            )
            _uiEvents.emit("رکورد خواب و استراحت ذخیره شد")
        }
    }

    fun saveReminder(
        context: Context,
        id: Long = 0,
        title: String,
        dateIso: String,
        timeHm: String,
        triggerEpochMillis: Long,
        repeatInterval: RepeatInterval,
        isEnabled: Boolean,
        category: String
    ) {
        if (title.isBlank()) {
            viewModelScope.launch { _uiEvents.emit("خطا: عنوان یادآوری الزامی است") }
            return
        }
        viewModelScope.launch {
            val savedId = repository.upsertReminder(
                ReminderEntity(
                    id = id,
                    title = title.trim(),
                    dateIso = dateIso,
                    timeHm = timeHm,
                    triggerEpochMillis = triggerEpochMillis,
                    repeatInterval = repeatInterval,
                    isEnabled = isEnabled,
                    category = category
                )
            )
            val resolvedId = if (id != 0L) id else savedId
            ReminderScheduler.scheduleReminder(
                context,
                ReminderEntity(
                    id = resolvedId,
                    title = title.trim(),
                    dateIso = dateIso,
                    timeHm = timeHm,
                    triggerEpochMillis = triggerEpochMillis,
                    repeatInterval = repeatInterval,
                    isEnabled = isEnabled,
                    category = category
                )
            )
            _uiEvents.emit("یادآوری ذخیره و اعلان آن زمان‌بندی شد")
        }
    }

    fun updateThemeMode(mode: ThemeModePreference) {
        viewModelScope.launch { preferencesRepository.updateThemeMode(mode) }
    }

    fun updatePrimaryColor(colorKey: String) {
        viewModelScope.launch { preferencesRepository.updatePrimaryColor(colorKey) }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch { preferencesRepository.updateFontScale(scale) }
    }
}
