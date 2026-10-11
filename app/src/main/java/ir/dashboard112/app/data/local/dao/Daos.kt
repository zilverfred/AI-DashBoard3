package ir.dashboard112.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.dashboard112.app.data.local.entity.FinanceTransactionEntity
import ir.dashboard112.app.data.local.entity.OfficeTaskEntity
import ir.dashboard112.app.data.local.entity.ReminderEntity
import ir.dashboard112.app.data.local.entity.SleepRecordEntity
import ir.dashboard112.app.data.local.entity.TaskEntity
import ir.dashboard112.app.data.local.entity.VehicleServiceEntity
import ir.dashboard112.app.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority ASC, createdAtEpochMillis DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateIso = :dateIso ORDER BY isCompleted ASC, priority ASC")
    fun observeTasksByDate(dateIso: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks")
    suspend fun getAllSnapshot(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY dateIso DESC, createdAtEpochMillis DESC")
    fun observeAllWorkouts(): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(workout: WorkoutEntity): Long

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts")
    suspend fun getAllSnapshot(): List<WorkoutEntity>

    @Query("DELETE FROM workouts")
    suspend fun clearAll()
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_transactions ORDER BY dateIso DESC, createdAtEpochMillis DESC")
    fun observeAllTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE jalaliYearMonth = :jalaliYearMonth ORDER BY dateIso DESC")
    fun observeTransactionsByMonth(jalaliYearMonth: String): Flow<List<FinanceTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: FinanceTransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: FinanceTransactionEntity)

    @Query("DELETE FROM finance_transactions WHERE linkedVehicleServiceId = :vehicleServiceId")
    suspend fun deleteByLinkedVehicleService(vehicleServiceId: Long)

    @Query("SELECT * FROM finance_transactions")
    suspend fun getAllSnapshot(): List<FinanceTransactionEntity>

    @Query("DELETE FROM finance_transactions")
    suspend fun clearAll()
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle_services ORDER BY isResolved ASC, dueDateIso ASC")
    fun observeAllVehicleServices(): Flow<List<VehicleServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicleService(service: VehicleServiceEntity): Long

    @Delete
    suspend fun deleteVehicleService(service: VehicleServiceEntity)

    @Query("SELECT * FROM vehicle_services")
    suspend fun getAllSnapshot(): List<VehicleServiceEntity>

    @Query("DELETE FROM vehicle_services")
    suspend fun clearAll()
}

@Dao
interface OfficeTaskDao {
    @Query("SELECT * FROM office_tasks ORDER BY isCompleted ASC, deadlineEpochMillis ASC")
    fun observeAllOfficeTasks(): Flow<List<OfficeTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfficeTask(officeTask: OfficeTaskEntity): Long

    @Delete
    suspend fun deleteOfficeTask(officeTask: OfficeTaskEntity)

    @Query("SELECT * FROM office_tasks")
    suspend fun getAllSnapshot(): List<OfficeTaskEntity>

    @Query("DELETE FROM office_tasks")
    suspend fun clearAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerEpochMillis ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getEnabledRemindersSnapshot(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity): Long

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders")
    suspend fun getAllSnapshot(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun clearAll()
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records ORDER BY dateIso DESC")
    fun observeAllSleepRecords(): Flow<List<SleepRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepRecord(record: SleepRecordEntity): Long

    @Delete
    suspend fun deleteSleepRecord(record: SleepRecordEntity)

    @Query("SELECT * FROM sleep_records")
    suspend fun getAllSnapshot(): List<SleepRecordEntity>

    @Query("DELETE FROM sleep_records")
    suspend fun clearAll()
}
