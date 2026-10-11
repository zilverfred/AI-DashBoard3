package ir.dashboard112.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.dashboard112.app.data.local.dao.FinanceDao
import ir.dashboard112.app.data.local.dao.OfficeTaskDao
import ir.dashboard112.app.data.local.dao.ReminderDao
import ir.dashboard112.app.data.local.dao.SleepDao
import ir.dashboard112.app.data.local.dao.TaskDao
import ir.dashboard112.app.data.local.dao.VehicleDao
import ir.dashboard112.app.data.local.dao.WorkoutDao
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

class DatabaseTypeConverters {
    @TypeConverter
    fun fromPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority =
        runCatching { TaskPriority.valueOf(value) }.getOrDefault(TaskPriority.NORMAL)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromRepeatInterval(value: RepeatInterval): String = value.name

    @TypeConverter
    fun toRepeatInterval(value: String): RepeatInterval =
        runCatching { RepeatInterval.valueOf(value) }.getOrDefault(RepeatInterval.NONE)

    @TypeConverter
    fun fromVehicleUrgency(value: VehicleUrgency): String = value.name

    @TypeConverter
    fun toVehicleUrgency(value: String): VehicleUrgency =
        runCatching { VehicleUrgency.valueOf(value) }.getOrDefault(VehicleUrgency.UPCOMING_SERVICE)
}

@Database(
    entities = [
        TaskEntity::class,
        WorkoutEntity::class,
        FinanceTransactionEntity::class,
        VehicleServiceEntity::class,
        OfficeTaskEntity::class,
        ReminderEntity::class,
        SleepRecordEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(DatabaseTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun financeDao(): FinanceDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun officeTaskDao(): OfficeTaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun sleepDao(): SleepDao

    companion object {
        private const val DATABASE_NAME = "dashboard112.db"

        /**
         * مهاجرت مستند و قابل تست از نسخه ۱ به نسخه ۲:
         * افزودن ستون linkedVehicleServiceId به جدول تراکنش‌های مالی برای اتصال هزینه‌های خودرو به گزارش مالی.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE finance_transactions ADD COLUMN linkedVehicleServiceId INTEGER DEFAULT NULL"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
