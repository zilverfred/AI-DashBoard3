package ir.dashboard112.app

import ir.dashboard112.app.data.local.entity.FinanceTransactionEntity
import ir.dashboard112.app.data.local.entity.OfficeTaskEntity
import ir.dashboard112.app.data.local.entity.TaskEntity
import ir.dashboard112.app.data.local.entity.TaskPriority
import ir.dashboard112.app.data.local.entity.TransactionType
import ir.dashboard112.app.data.local.entity.VehicleServiceEntity
import ir.dashboard112.app.data.local.entity.VehicleUrgency
import ir.dashboard112.app.domain.util.FinanceCalculator
import ir.dashboard112.app.domain.util.OfficeTaskEvaluator
import ir.dashboard112.app.domain.util.PersianDateUtils
import ir.dashboard112.app.domain.util.ProgressCalculator
import ir.dashboard112.app.domain.util.SleepCalculator
import ir.dashboard112.app.domain.util.VehicleHealthEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicTest {

    @Test
    fun jalaliConversion_convertsStandardGregorianDateCorrectly() {
        val jalali = PersianDateUtils.gregorianToJalali(2026, 10, 10)
        assertEquals(1405, jalali.year)
        assertEquals(7, jalali.month)
        assertEquals(18, jalali.day)
        assertEquals("1405-07", jalali.formatYearMonth())
    }

    @Test
    fun sleepCalculator_handlesCrossMidnightIntervalCorrectly() {
        val result = SleepCalculator.calculateDurationMinutes("23:15", "06:45")
        assertTrue(result.isSuccess)
        assertEquals(450, result.getOrNull()) // 7 hours 30 minutes = 450 minutes
    }

    @Test
    fun sleepCalculator_handlesSameDayIntervalCorrectly() {
        val result = SleepCalculator.calculateDurationMinutes("01:00", "08:30")
        assertTrue(result.isSuccess)
        assertEquals(450, result.getOrNull())
    }

    @Test
    fun sleepCalculator_rejectsIdenticalOrInvalidTimes() {
        assertTrue(SleepCalculator.calculateDurationMinutes("22:00", "22:00").isFailure)
        assertTrue(SleepCalculator.calculateDurationMinutes("25:00", "07:00").isFailure)
        assertTrue(SleepCalculator.calculateDurationMinutes("invalid", "07:00").isFailure)
    }

    @Test
    fun dailyProgress_calculatesZeroWhenEmpty_andExactPercentageWhenPopulated() {
        assertEquals(0, ProgressCalculator.calculateDailyProgress(emptyList()))

        val sampleTasks = listOf(
            TaskEntity(id = 1, title = "کار ۱", priority = TaskPriority.CRITICAL, dateIso = "2026-10-10", isCompleted = true),
            TaskEntity(id = 2, title = "کار ۲", priority = TaskPriority.IMPORTANT, dateIso = "2026-10-10", isCompleted = true),
            TaskEntity(id = 3, title = "کار ۳", priority = TaskPriority.NORMAL, dateIso = "2026-10-10", isCompleted = false),
            TaskEntity(id = 4, title = "کار ۴", priority = TaskPriority.NORMAL, dateIso = "2026-10-10", isCompleted = false)
        )
        assertEquals(50, ProgressCalculator.calculateDailyProgress(sampleTasks))
    }

    @Test
    fun financeCalculator_computesExactLongSumsWithoutRoundingError() {
        val txs = listOf(
            FinanceTransactionEntity(
                id = 1,
                title = "حقوق مهرماه",
                amountTomans = 42_500_000L,
                type = TransactionType.INCOME,
                category = "حقوق",
                dateIso = "2026-10-10",
                jalaliYearMonth = "1405-07"
            ),
            FinanceTransactionEntity(
                id = 2,
                title = "سرویس خودرو",
                amountTomans = 2_350_000L,
                type = TransactionType.EXPENSE,
                category = "خودرو",
                dateIso = "2026-10-10",
                jalaliYearMonth = "1405-07"
            ),
            FinanceTransactionEntity(
                id = 3,
                title = "اجاره شهریور",
                amountTomans = 12_000_000L,
                type = TransactionType.EXPENSE,
                category = "مسکن",
                dateIso = "2026-09-10",
                jalaliYearMonth = "1405-06"
            )
        )
        val summary = FinanceCalculator.calculateSummary(txs, "2026-10-10", "1405-07")
        assertEquals(42_500_000L, summary.todayIncome)
        assertEquals(2_350_000L, summary.todayExpense)
        assertEquals(42_500_000L, summary.selectedMonthIncome)
        assertEquals(2_350_000L, summary.selectedMonthExpense)
        assertEquals(28_150_000L, summary.netBalance)
    }

    @Test
    fun officeTaskEvaluator_detectsOverdueOnlyWhenIncompleteAndPastDeadline() {
        val now = 1_760_000_000_000L
        val overdueTask = OfficeTaskEntity(
            id = 1,
            title = "ارسال گزارش مالیاتی",
            departmentOrOrganization = "امور مالی",
            deadlineIso = "2026-10-08",
            deadlineTimeHm = "14:00",
            deadlineEpochMillis = now - 3_600_000L,
            isCompleted = false
        )
        val completedPastTask = overdueTask.copy(isCompleted = true)
        assertTrue(OfficeTaskEvaluator.isOverdue(overdueTask, now))
        assertFalse(OfficeTaskEvaluator.isOverdue(completedPastTask, now))
    }

    @Test
    fun vehicleHealthEvaluator_identifiesDueSoonByKilometerOrDate() {
        val service = VehicleServiceEntity(
            id = 1,
            vehicleName = "پژو ۲۰۷",
            serviceTitle = "تعویض تسمه تایم",
            urgency = VehicleUrgency.UPCOMING_SERVICE,
            currentOdometerKm = 59_400,
            dueOdometerKm = 60_000,
            costTomans = 3_200_000L,
            dueDateIso = "2026-11-01",
            isResolved = false
        )
        assertTrue(VehicleHealthEvaluator.isServiceDueSoon(service, "2026-10-10", kmThreshold = 1000))
    }
}
