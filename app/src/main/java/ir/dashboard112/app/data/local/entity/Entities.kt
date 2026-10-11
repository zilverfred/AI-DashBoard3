package ir.dashboard112.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TaskPriority(val orderWeight: Int, val labelFa: String) {
    CRITICAL(1, "ضروری"),
    IMPORTANT(2, "مهم"),
    NORMAL(3, "عادی")
}

@Serializable
enum class TransactionType(val labelFa: String) {
    INCOME("درآمد"),
    EXPENSE("هزینه")
}

@Serializable
enum class RepeatInterval(val labelFa: String) {
    NONE("یک‌بار"),
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه")
}

@Serializable
enum class VehicleUrgency(val labelFa: String) {
    ESSENTIAL_REPAIR("تعمیر ضروری"),
    UPCOMING_SERVICE("سرویس دوره‌ای"),
    ROUTINE_CHECK("بررسی عادی")
}

@Serializable
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["dateIso", "priority", "isCompleted"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val priority: TaskPriority = TaskPriority.NORMAL,
    val dateIso: String, // YYYY-MM-DD in device local timezone
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "workouts",
    indices = [Index(value = ["dateIso"])]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutType: String,
    val durationMinutes: Int,
    val bodyWeightKg: Double,
    val personalRecordText: String,
    val targetProgressPercent: Int,
    val dateIso: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "finance_transactions",
    indices = [Index(value = ["dateIso", "type", "category"])]
)
data class FinanceTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountTomans: Long, // ذخیره به صورت عدد صحیح Long برای جلوگیری کامل از خطای گرد کردن
    val type: TransactionType,
    val category: String,
    val dateIso: String, // YYYY-MM-DD
    val jalaliYearMonth: String, // e.g., "1405-07"
    val description: String = "",
    val linkedVehicleServiceId: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "vehicle_services",
    indices = [Index(value = ["dueDateIso", "urgency"])]
)
data class VehicleServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleName: String,
    val serviceTitle: String,
    val urgency: VehicleUrgency,
    val currentOdometerKm: Int,
    val dueOdometerKm: Int,
    val costTomans: Long,
    val dueDateIso: String,
    val isResolved: Boolean = false,
    val notes: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "office_tasks",
    indices = [Index(value = ["deadlineEpochMillis", "isCompleted"])]
)
data class OfficeTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val departmentOrOrganization: String,
    val priority: TaskPriority = TaskPriority.IMPORTANT,
    val deadlineIso: String, // YYYY-MM-DD
    val deadlineTimeHm: String, // HH:mm
    val deadlineEpochMillis: Long,
    val isCompleted: Boolean = false,
    val reminderTimeHm: String? = null,
    val notes: String = ""
)

@Serializable
@Entity(
    tableName = "reminders",
    indices = [Index(value = ["triggerEpochMillis", "isEnabled"])]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateIso: String,
    val timeHm: String, // HH:mm
    val triggerEpochMillis: Long,
    val repeatInterval: RepeatInterval = RepeatInterval.NONE,
    val isEnabled: Boolean = true,
    val category: String = "عمومی"
)

@Serializable
@Entity(
    tableName = "sleep_records",
    indices = [Index(value = ["dateIso"])]
)
data class SleepRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val sleepStartHm: String, // HH:mm (e.g. "23:15")
    val wakeUpHm: String,     // HH:mm (e.g. "06:45")
    val sleepDurationMinutes: Int,
    val restAndLeisureMinutes: Int,
    val qualityRating: Int = 4, // 1..5
    val notes: String = ""
)
