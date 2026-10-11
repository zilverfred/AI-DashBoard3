package ir.dashboard112.app.domain.util

import ir.dashboard112.app.data.local.entity.FinanceTransactionEntity
import ir.dashboard112.app.data.local.entity.OfficeTaskEntity
import ir.dashboard112.app.data.local.entity.TaskEntity
import ir.dashboard112.app.data.local.entity.TransactionType
import ir.dashboard112.app.data.local.entity.VehicleServiceEntity
import ir.dashboard112.app.data.local.entity.VehicleUrgency
import java.time.LocalDate

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun formatFullTitle(): String = "$day ${monthNames[(month - 1).coerceIn(0, 11)]} $year"
    fun formatYearMonth(): String = "%04d-%02d".format(year, month)
}

object PersianDateUtils {
    /**
     * تبدیل دقیق تاریخ میلادی به تاریخ هجری شمسی (جلالی) بدون وابستگی خارجی
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[(gm - 1).coerceIn(0, 11)]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun isoDateToJalaliYearMonth(isoDate: String): String {
        val parsed = runCatching { LocalDate.parse(isoDate) }.getOrDefault(LocalDate.now())
        return gregorianToJalali(parsed.year, parsed.monthValue, parsed.dayOfMonth).formatYearMonth()
    }
}

object SleepCalculator {
    /**
     * محاسبه دقیق مدت خواب بر حسب دقیقه با پشتیبانی کامل از بازه‌های عبوری از نیمه‌شب (مثلاً 23:30 تا 06:45)
     */
    fun calculateDurationMinutes(sleepStartHm: String, wakeUpHm: String): Result<Int> {
        val startParts = sleepStartHm.trim().split(":")
        val wakeParts = wakeUpHm.trim().split(":")
        if (startParts.size != 2 || wakeParts.size != 2) {
            return Result.failure(IllegalArgumentException("قالب ساعت باید به صورت HH:mm باشد"))
        }
        val sh = startParts[0].toIntOrNull()
        val sm = startParts[1].toIntOrNull()
        val wh = wakeParts[0].toIntOrNull()
        val wm = wakeParts[1].toIntOrNull()
        if (sh == null || sm == null || wh == null || wm == null ||
            sh !in 0..23 || wh !in 0..23 || sm !in 0..59 || wm !in 0..59
        ) {
            return Result.failure(IllegalArgumentException("مقادیر ساعت یا دقیقه نامعتبر است"))
        }
        val startTotal = sh * 60 + sm
        val wakeTotal = wh * 60 + wm
        if (startTotal == wakeTotal) {
            return Result.failure(IllegalArgumentException("ساعت خواب و بیداری نمی‌تواند یکسان باشد"))
        }
        val duration = if (wakeTotal > startTotal) {
            wakeTotal - startTotal
        } else {
            (24 * 60 - startTotal) + wakeTotal
        }
        if (duration > 20 * 60) {
            return Result.failure(IllegalArgumentException("مدت خواب بیش از ۲۰ ساعت منطقی نیست"))
        }
        return Result.success(duration)
    }

    fun formatMinutesFa(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0) "${hours} ساعت" else "${hours} ساعت و ${minutes} دقیقه"
    }
}

object ProgressCalculator {
    /**
     * محاسبه پایدار و مستقل از UI برای درصد پیشرفت کارهای روزانه (0..100)
     */
    fun calculateDailyProgress(todayTasks: List<TaskEntity>): Int {
        if (todayTasks.isEmpty()) return 0
        val completed = todayTasks.count { it.isCompleted }
        return ((completed.toDouble() / todayTasks.size.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }

    fun evaluateDayStatusLabel(progressPercent: Int, totalTasks: Int): String = when {
        totalTasks == 0 -> "بدون کار ثبت‌شده برای امروز"
        progressPercent == 100 -> "عالی — تمام کارهای امروز تکمیل شد"
        progressPercent >= 70 -> "مطلوب — در مسیر تکمیل برنامه روزانه"
        progressPercent >= 35 -> "در جریان — نیازمند تمرکز روی اولویت‌های ضروری"
        else -> "آغاز روز — کارهای مهم در انتظار اقدام"
    }
}

object OfficeTaskEvaluator {
    /**
     * تشخیص خودکار عقب‌افتاده بودن وظایف اداری بر اساس زمان فعلی دستگاه و وضعیت تکمیل
     */
    fun isOverdue(task: OfficeTaskEntity, nowEpochMillis: Long): Boolean {
        return !task.isCompleted && task.deadlineEpochMillis < nowEpochMillis
    }
}

data class FinanceSummary(
    val todayIncome: Long,
    val todayExpense: Long,
    val totalIncome: Long,
    val totalExpense: Long,
    val netBalance: Long,
    val selectedMonthIncome: Long,
    val selectedMonthExpense: Long
)

object FinanceCalculator {
    /**
     * محاسبه دقیق مالی با اعداد صحیح Long بدون هیچ‌گونه خطای ممیز شناور یا گرد کردن
     */
    fun calculateSummary(
        transactions: List<FinanceTransactionEntity>,
        todayIso: String,
        selectedJalaliYearMonth: String
    ): FinanceSummary {
        var todayIncome = 0L
        var todayExpense = 0L
        var totalIncome = 0L
        var totalExpense = 0L
        var monthIncome = 0L
        var monthExpense = 0L

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalIncome += tx.amountTomans
                if (tx.dateIso == todayIso) todayIncome += tx.amountTomans
                if (tx.jalaliYearMonth == selectedJalaliYearMonth) monthIncome += tx.amountTomans
            } else {
                totalExpense += tx.amountTomans
                if (tx.dateIso == todayIso) todayExpense += tx.amountTomans
                if (tx.jalaliYearMonth == selectedJalaliYearMonth) monthExpense += tx.amountTomans
            }
        }
        return FinanceSummary(
            todayIncome = todayIncome,
            todayExpense = todayExpense,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            selectedMonthIncome = monthIncome,
            selectedMonthExpense = monthExpense
        )
    }
}

object VehicleHealthEvaluator {
    fun isServiceDueSoon(
        service: VehicleServiceEntity,
        todayIso: String,
        kmThreshold: Int = 1000
    ): Boolean {
        if (service.isResolved) return false
        val remainingKm = service.dueOdometerKm - service.currentOdometerKm
        return remainingKm <= kmThreshold || service.dueDateIso <= todayIso
    }

    fun evaluateOverallVehicleStatus(
        services: List<VehicleServiceEntity>,
        todayIso: String
    ): String {
        val active = services.filter { !it.isResolved }
        if (active.isEmpty()) return "سالم — بدون سرویس یا تعمیر باز"
        val essentialCount = active.count { it.urgency == VehicleUrgency.ESSENTIAL_REPAIR }
        val dueSoonCount = active.count { isServiceDueSoon(it, todayIso) }
        return when {
            essentialCount > 0 -> "نیازمند رسیدگی فوری ($essentialCount تعمیر ضروری)"
            dueSoonCount > 0 -> "سرویس نزدیک ($dueSoonCount مورد در آستانه سررسید)"
            else -> "نرمال — سرویس‌ها طبق برنامه"
        }
    }
}
