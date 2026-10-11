package ir.dashboard112.app.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ir.dashboard112.app.MainActivity
import ir.dashboard112.app.R
import ir.dashboard112.app.data.local.AppDatabase
import ir.dashboard112.app.data.local.entity.ReminderEntity
import ir.dashboard112.app.data.local.entity.RepeatInterval
import ir.dashboard112.app.data.local.entity.TaskPriority
import ir.dashboard112.app.data.local.entity.VehicleServiceEntity
import ir.dashboard112.app.domain.util.VehicleHealthEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class NotificationPriorityLevel(
    val channelId: String,
    val channelNameFa: String,
    val importance: Int,
    val compatPriority: Int
) {
    CRITICAL(
        channelId = "dashboard112_channel_critical",
        channelNameFa = "هشدارهای فوری (کارهای ضروری و تعمیرات خودرو)",
        importance = NotificationManager.IMPORTANCE_HIGH,
        compatPriority = NotificationCompat.PRIORITY_HIGH
    ),
    STANDARD(
        channelId = "dashboard112_channel_standard",
        channelNameFa = "یادآوری‌های روزانه و جلسات ورزشی",
        importance = NotificationManager.IMPORTANCE_DEFAULT,
        compatPriority = NotificationCompat.PRIORITY_DEFAULT
    ),
    LOW(
        channelId = "dashboard112_channel_low",
        channelNameFa = "اطلاع‌رسانی‌های عادی و گزارش‌ها",
        importance = NotificationManager.IMPORTANCE_LOW,
        compatPriority = NotificationCompat.PRIORITY_LOW
    )
}

object ReminderScheduler {
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_PRIORITY_LEVEL = "extra_priority_level"
    const val EXTRA_REPEAT_INTERVAL = "extra_repeat_interval"

    /**
     * بررسی وضعیت مجوز اعلان در اندروید ۱۳ به بالا (API 33+) و فعال بودن اعلان‌ها در سطح سیستم
     */
    fun hasNotificationPermission(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * ایجاد کانال‌های اعلان مجزا بر اساس سطح اولویت (ضروری، استاندارد، عادی)
     */
    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            NotificationPriorityLevel.entries.forEach { level ->
                val channel = NotificationChannel(
                    level.channelId,
                    level.channelNameFa,
                    level.importance
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    enableVibration(level == NotificationPriorityLevel.CRITICAL)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * زمان‌بندی عمومی و پایدار یادآوری‌ها (شامل یادآوری کارها، ورزش، سرویس خودرو و یادآوری تکرارشونده)
     */
    fun scheduleReminder(
        context: Context,
        reminder: ReminderEntity,
        priorityLevel: NotificationPriorityLevel = NotificationPriorityLevel.STANDARD
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val stableRequestCode = reminder.id.toInt()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, stableRequestCode)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_MESSAGE, "دسته‌بندی: ${reminder.category} · ساعت ${reminder.timeHm}")
            putExtra(EXTRA_CATEGORY, reminder.category)
            putExtra(EXTRA_PRIORITY_LEVEL, priorityLevel.name)
            putExtra(EXTRA_REPEAT_INTERVAL, reminder.repeatInterval.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!reminder.isEnabled || reminder.triggerEpochMillis <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerEpochMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerEpochMillis,
                pendingIntent
            )
        }
    }

    /**
     * زمان‌بندی هشدار سرویس یا تعمیر ضروری خودرو
     */
    fun scheduleVehicleServiceAlert(
        context: Context,
        service: VehicleServiceEntity,
        triggerEpochMillis: Long
    ) {
        val stableId = 500_000L + service.id
        val priority = if (service.urgency.name == "ESSENTIAL_REPAIR") {
            NotificationPriorityLevel.CRITICAL
        } else {
            NotificationPriorityLevel.STANDARD
        }
        scheduleReminder(
            context = context,
            reminder = ReminderEntity(
                id = stableId,
                title = "هشدار خودرو: ${service.serviceTitle} (${service.vehicleName})",
                dateIso = service.dueDateIso,
                timeHm = "09:00",
                triggerEpochMillis = triggerEpochMillis,
                repeatInterval = RepeatInterval.NONE,
                isEnabled = !service.isResolved,
                category = "خودرو"
            ),
            priorityLevel = priority
        )
    }

    /**
     * لغو قطعی اعلان زمان‌بندی‌شده هنگام حذف یا غیرفعال‌سازی یادآوری
     */
    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * محاسبه زمان بعدی برای یادآوری‌های تکرارشونده (روزانه، هفتگی، ماهانه)
     */
    fun calculateNextRecurringTrigger(currentTriggerMillis: Long, repeatInterval: RepeatInterval): Long {
        val deltaMillis = when (repeatInterval) {
            RepeatInterval.DAILY -> 24L * 60L * 60L * 1000L
            RepeatInterval.WEEKLY -> 7L * 24L * 60L * 60L * 1000L
            RepeatInterval.MONTHLY -> 30L * 24L * 60L * 60L * 1000L
            RepeatInterval.NONE -> 0L
        }
        return if (deltaMillis > 0L) currentTriggerMillis + deltaMillis else currentTriggerMillis
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.ensureNotificationChannels(context)
        if (!ReminderScheduler.hasNotificationPermission(context)) return

        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 1001)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "یادآوری Dashboard 112"
        val message = intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE) ?: ""
        val category = intent.getStringExtra(ReminderScheduler.EXTRA_CATEGORY) ?: "عمومی"
        val priorityName = intent.getStringExtra(ReminderScheduler.EXTRA_PRIORITY_LEVEL)
            ?: NotificationPriorityLevel.STANDARD.name
        val repeatName = intent.getStringExtra(ReminderScheduler.EXTRA_REPEAT_INTERVAL)
            ?: RepeatInterval.NONE.name

        val priorityLevel = runCatching { NotificationPriorityLevel.valueOf(priorityName) }
            .getOrDefault(NotificationPriorityLevel.STANDARD)
        val repeatInterval = runCatching { RepeatInterval.valueOf(repeatName) }
            .getOrDefault(RepeatInterval.NONE)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, priorityLevel.channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setSubText(category)
            .setPriority(priorityLevel.compatPriority)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)

        // زمان‌بندی مجدد خودکار برای یادآوری‌های تکرارشونده
        if (repeatInterval != RepeatInterval.NONE) {
            val nextTrigger = ReminderScheduler.calculateNextRecurringTrigger(
                System.currentTimeMillis(),
                repeatInterval
            )
            ReminderScheduler.scheduleReminder(
                context = context,
                reminder = ReminderEntity(
                    id = notificationId.toLong(),
                    title = title,
                    dateIso = "",
                    timeHm = "",
                    triggerEpochMillis = nextTrigger,
                    repeatInterval = repeatInterval,
                    isEnabled = true,
                    category = category
                ),
                priorityLevel = priorityLevel
            )
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (intent.action !in validActions) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val now = System.currentTimeMillis()
            val todayIso = LocalDate.now().toString()

            // ۱. بازگردانی تمام یادآوری‌های فعال آینده و تکرارشونده
            db.reminderDao().getEnabledRemindersSnapshot().forEach { reminder ->
                val trigger = if (reminder.triggerEpochMillis > now) {
                    reminder.triggerEpochMillis
                } else if (reminder.repeatInterval != RepeatInterval.NONE) {
                    ReminderScheduler.calculateNextRecurringTrigger(now, reminder.repeatInterval)
                } else {
                    0L
                }
                if (trigger > now) {
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        reminder = reminder.copy(triggerEpochMillis = trigger)
                    )
                }
            }

            // ۲. بازگردانی هشدارهای سرویس خودروی نزدیک یا ضروری
            db.vehicleDao().getAllSnapshot().forEach { service ->
                if (!service.isResolved && VehicleHealthEvaluator.isServiceDueSoon(service, todayIso)) {
                    ReminderScheduler.scheduleVehicleServiceAlert(
                        context = context,
                        service = service,
                        triggerEpochMillis = now + 60_000L
                    )
                }
            }
        }
    }
}
