package ir.dashboard112.app.data.repository

import androidx.room.withTransaction
import ir.dashboard112.app.data.local.AppDatabase
import ir.dashboard112.app.data.local.entity.FinanceTransactionEntity
import ir.dashboard112.app.data.local.entity.OfficeTaskEntity
import ir.dashboard112.app.data.local.entity.ReminderEntity
import ir.dashboard112.app.data.local.entity.SleepRecordEntity
import ir.dashboard112.app.data.local.entity.TaskEntity
import ir.dashboard112.app.data.local.entity.TransactionType
import ir.dashboard112.app.data.local.entity.VehicleServiceEntity
import ir.dashboard112.app.data.local.entity.WorkoutEntity
import ir.dashboard112.app.domain.util.PersianDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DashboardBackupPayload(
    val schemaVersion: Int = 2,
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.2.0",
    val tasks: List<TaskEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val financeTransactions: List<FinanceTransactionEntity> = emptyList(),
    val vehicleServices: List<VehicleServiceEntity> = emptyList(),
    val officeTasks: List<OfficeTaskEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val sleepRecords: List<SleepRecordEntity> = emptyList()
)

class DashboardRepository(private val database: AppDatabase) {
    private val jsonFormatter = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val tasksFlow: Flow<List<TaskEntity>> = database.taskDao().observeAllTasks()
    val workoutsFlow: Flow<List<WorkoutEntity>> = database.workoutDao().observeAllWorkouts()
    val financeFlow: Flow<List<FinanceTransactionEntity>> = database.financeDao().observeAllTransactions()
    val vehicleFlow: Flow<List<VehicleServiceEntity>> = database.vehicleDao().observeAllVehicleServices()
    val officeTasksFlow: Flow<List<OfficeTaskEntity>> = database.officeTaskDao().observeAllOfficeTasks()
    val remindersFlow: Flow<List<ReminderEntity>> = database.reminderDao().observeAllReminders()
    val sleepRecordsFlow: Flow<List<SleepRecordEntity>> = database.sleepDao().observeAllSleepRecords()

    suspend fun upsertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        database.taskDao().upsertTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        database.taskDao().deleteTask(task)
    }

    suspend fun upsertWorkout(workout: WorkoutEntity): Long = withContext(Dispatchers.IO) {
        database.workoutDao().upsertWorkout(workout)
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) = withContext(Dispatchers.IO) {
        database.workoutDao().deleteWorkout(workout)
    }

    suspend fun upsertFinanceTransaction(tx: FinanceTransactionEntity): Long = withContext(Dispatchers.IO) {
        database.financeDao().upsertTransaction(tx)
    }

    suspend fun deleteFinanceTransaction(tx: FinanceTransactionEntity) = withContext(Dispatchers.IO) {
        database.financeDao().deleteTransaction(tx)
    }

    /**
     * ثبت یا ویرایش سرویس خودرو همراه با همگام‌سازی خودکار هزینه آن در بخش مالی
     */
    suspend fun upsertVehicleServiceWithFinanceSync(service: VehicleServiceEntity): Long =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val serviceId = database.vehicleDao().upsertVehicleService(service)
                val resolvedId = if (service.id != 0L) service.id else serviceId
                database.financeDao().deleteByLinkedVehicleService(resolvedId)
                if (service.costTomans > 0L) {
                    val jalaliMonth = PersianDateUtils.isoDateToJalaliYearMonth(service.dueDateIso)
                    database.financeDao().upsertTransaction(
                        FinanceTransactionEntity(
                            title = "${service.serviceTitle} (${service.vehicleName})",
                            amountTomans = service.costTomans,
                            type = TransactionType.EXPENSE,
                            category = "خودرو",
                            dateIso = service.dueDateIso,
                            jalaliYearMonth = jalaliMonth,
                            description = "ثبت خودکار از بخش خودرو - کیلومتر ${service.currentOdometerKm}",
                            linkedVehicleServiceId = resolvedId
                        )
                    )
                }
                resolvedId
            }
        }

    suspend fun deleteVehicleService(service: VehicleServiceEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.financeDao().deleteByLinkedVehicleService(service.id)
            database.vehicleDao().deleteVehicleService(service)
        }
    }

    suspend fun upsertOfficeTask(task: OfficeTaskEntity): Long = withContext(Dispatchers.IO) {
        database.officeTaskDao().upsertOfficeTask(task)
    }

    suspend fun deleteOfficeTask(task: OfficeTaskEntity) = withContext(Dispatchers.IO) {
        database.officeTaskDao().deleteOfficeTask(task)
    }

    suspend fun upsertReminder(reminder: ReminderEntity): Long = withContext(Dispatchers.IO) {
        database.reminderDao().upsertReminder(reminder)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        database.reminderDao().deleteReminder(reminder)
    }

    suspend fun upsertSleepRecord(record: SleepRecordEntity): Long = withContext(Dispatchers.IO) {
        database.sleepDao().upsertSleepRecord(record)
    }

    suspend fun deleteSleepRecord(record: SleepRecordEntity) = withContext(Dispatchers.IO) {
        database.sleepDao().deleteSleepRecord(record)
    }

    /**
     * استخراج تمام اطلاعات پایگاه داده به قالب استاندارد JSON خارج از Main Thread
     */
    suspend fun exportBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = DashboardBackupPayload(
                tasks = database.taskDao().getAllSnapshot(),
                workouts = database.workoutDao().getAllSnapshot(),
                financeTransactions = database.financeDao().getAllSnapshot(),
                vehicleServices = database.vehicleDao().getAllSnapshot(),
                officeTasks = database.officeTaskDao().getAllSnapshot(),
                reminders = database.reminderDao().getAllSnapshot(),
                sleepRecords = database.sleepDao().getAllSnapshot()
            )
            jsonFormatter.encodeToString(payload)
        }
    }

    /**
     * بازیابی تراکنشی و ایمن اطلاعات از فایل JSON پشتیبان خارج از Main Thread
     */
    suspend fun importBackupJson(rawJson: String, replaceExisting: Boolean = true): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = jsonFormatter.decodeFromString<DashboardBackupPayload>(rawJson)
                require(payload.schemaVersion in 1..2) { "نسخه قالب فایل پشتیبان پشتیبانی نمی‌شود" }
                var totalImported = 0
                database.withTransaction {
                    if (replaceExisting) {
                        database.taskDao().clearAll()
                        database.workoutDao().clearAll()
                        database.financeDao().clearAll()
                        database.vehicleDao().clearAll()
                        database.officeTaskDao().clearAll()
                        database.reminderDao().clearAll()
                        database.sleepDao().clearAll()
                    }
                    payload.tasks.forEach { database.taskDao().upsertTask(it); totalImported++ }
                    payload.workouts.forEach { database.workoutDao().upsertWorkout(it); totalImported++ }
                    payload.financeTransactions.forEach { database.financeDao().upsertTransaction(it); totalImported++ }
                    payload.vehicleServices.forEach { database.vehicleDao().upsertVehicleService(it); totalImported++ }
                    payload.officeTasks.forEach { database.officeTaskDao().upsertOfficeTask(it); totalImported++ }
                    payload.reminders.forEach { database.reminderDao().upsertReminder(it); totalImported++ }
                    payload.sleepRecords.forEach { database.sleepDao().upsertSleepRecord(it); totalImported++ }
                }
                totalImported
            }
        }
}
