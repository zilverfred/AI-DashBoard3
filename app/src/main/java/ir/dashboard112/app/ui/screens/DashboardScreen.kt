package ir.dashboard112.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.dashboard112.app.data.local.entity.TaskEntity
import ir.dashboard112.app.domain.util.FinanceCalculator
import ir.dashboard112.app.domain.util.OfficeTaskEvaluator
import ir.dashboard112.app.domain.util.PersianDateUtils
import ir.dashboard112.app.domain.util.ProgressCalculator
import ir.dashboard112.app.domain.util.SleepCalculator
import ir.dashboard112.app.domain.util.VehicleHealthEvaluator
import ir.dashboard112.app.ui.viewmodel.DashboardUiState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DashboardHomeScreen(
    uiState: DashboardUiState,
    onNavigateToRoute: (String) -> Unit,
    onToggleTask: (TaskEntity) -> Unit
) {
    // ساعت زنده با به‌روزرسانی کم‌مصرف هر ۱ ثانیه بدون بازخوانی پایگاه داده
    var currentTimeHm by remember {
        mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
    }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeHm = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            delay(1_000L)
        }
    }

    val nowDate = LocalDate.now()
    val jalaliToday = remember(nowDate) {
        PersianDateUtils.gregorianToJalali(nowDate.year, nowDate.monthValue, nowDate.dayOfMonth)
    }
    val todayTasks = uiState.tasks.filter { it.dateIso == uiState.todayIso }
    val dailyProgress = ProgressCalculator.calculateDailyProgress(todayTasks)
    val dayStatusLabel = ProgressCalculator.evaluateDayStatusLabel(dailyProgress, todayTasks.size)
    val financeSummary = FinanceCalculator.calculateSummary(
        transactions = uiState.financeTransactions,
        todayIso = uiState.todayIso,
        selectedJalaliYearMonth = uiState.selectedJalaliMonth
    )
    val vehicleStatus = VehicleHealthEvaluator.evaluateOverallVehicleStatus(
        services = uiState.vehicleServices,
        todayIso = uiState.todayIso
    )
    val overdueOfficeCount = uiState.officeTasks.count {
        OfficeTaskEvaluator.isOverdue(it, System.currentTimeMillis())
    }
    val latestSleep = uiState.sleepRecords.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ۱. کارت نمای کلی امروز
        item {
            DashboardCardContainer(
                title = "۱. نمای کلی امروز",
                subtitle = "${jalaliToday.formatFullTitle()} · ساعت $currentTimeHm",
                onClick = { onNavigateToRoute("tasks") }
            ) {
                Text(text = dayStatusLabel, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { dailyProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "پیشرفت امروز: $dailyProgress٪ (${todayTasks.count { it.isCompleted }} از ${todayTasks.size} کار)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ۲. کارت کارهای امروز
        item {
            DashboardCardContainer(
                title = "۲. کارهای امروز",
                subtitle = "ضروری، مهم و عادی با امکان تیک‌زدن فوری",
                onClick = { onNavigateToRoute("tasks") }
            ) {
                if (todayTasks.isEmpty()) {
                    Text("هنوز کاری برای امروز ثبت نشده است. برای افزودن لمس کنید.")
                } else {
                    todayTasks.take(3).forEach { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onToggleTask(task) }
                            )
                            Text(
                                text = "${task.title} · ${task.priority.labelFa}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ۳. کارت ورزش
        item {
            val latestWorkout = uiState.workouts.firstOrNull()
            DashboardCardContainer(
                title = "۳. ورزش و سلامت",
                subtitle = "جلسات تمرینی هفته: ${uiState.workouts.size} از ${uiState.preferences.weeklyWorkoutTarget}",
                onClick = { onNavigateToRoute("workout") }
            ) {
                if (latestWorkout == null) {
                    Text("هنوز تمرینی ثبت نشده است.")
                } else {
                    Text("آخرین تمرین: ${latestWorkout.workoutType} (${latestWorkout.durationMinutes} دقیقه)")
                    Text("وزن: ${latestWorkout.bodyWeightKg} کیلوگرم · رکورد: ${latestWorkout.personalRecordText}")
                }
            }
        }

        // ۴. کارت مالی
        item {
            val unit = uiState.preferences.currencyUnit
            DashboardCardContainer(
                title = "۴. خلاصه مالی",
                subtitle = "مانده کل: ${financeSummary.netBalance} $unit",
                onClick = { onNavigateToRoute("finance") }
            ) {
                Text("درآمد امروز: ${financeSummary.todayIncome} $unit · هزینه امروز: ${financeSummary.todayExpense} $unit")
                Text("هزینه ماه جاری: ${financeSummary.selectedMonthExpense} $unit")
            }
        }

        // ۵. کارت خودرو
        item {
            DashboardCardContainer(
                title = "۵. وضعیت خودرو",
                subtitle = vehicleStatus,
                onClick = { onNavigateToRoute("vehicle") }
            ) {
                Text("تعداد رکوردهای سرویس و تعمیرات: ${uiState.vehicleServices.size} مورد")
            }
        }

        // ۶. کارت کار و اداره
        item {
            DashboardCardContainer(
                title = "۶. کار و اداره",
                subtitle = "وظایف اداری: ${uiState.officeTasks.size} مورد · عقب‌افتاده: $overdueOfficeCount مورد",
                onClick = { onNavigateToRoute("tasks") }
            ) {
                val nextOffice = uiState.officeTasks.firstOrNull { !it.isCompleted }
                Text(nextOffice?.let { "نزدیک‌ترین وظیفه: ${it.title} (${it.departmentOrOrganization})" }
                    ?: "تمام وظایف اداری انجام شده‌اند.")
            }
        }

        // ۷. کارت یادآوری‌ها
        item {
            val activeReminders = uiState.reminders.filter { it.isEnabled }
            DashboardCardContainer(
                title = "۷. یادآوری‌ها",
                subtitle = "${activeReminders.size} یادآوری فعال (روزانه، هفتگی و ماهانه)",
                onClick = { onNavigateToRoute("settings") }
            ) {
                val nextReminder = activeReminders.firstOrNull()
                Text(nextReminder?.let { "${it.title} · ساعت ${it.timeHm} (${it.repeatInterval.labelFa})" }
                    ?: "یادآوری فعالی ثبت نشده است.")
            }
        }

        // ۸. کارت خواب و استراحت
        item {
            DashboardCardContainer(
                title = "۸. خواب و استراحت",
                subtitle = latestSleep?.let {
                    "مدت خواب: ${SleepCalculator.formatMinutesFa(it.sleepDurationMinutes)}"
                } ?: "بدون رکورد خواب",
                onClick = { onNavigateToRoute("workout") }
            ) {
                if (latestSleep != null) {
                    Text("خواب: ${latestSleep.sleepStartHm} تا ${latestSleep.wakeUpHm} · استراحت و تفریح: ${latestSleep.restAndLeisureMinutes} دقیقه")
                } else {
                    Text("برای ثبت ساعت خواب و بیداری لمس کنید.")
                }
            }
        }
    }
}

@Composable
private fun DashboardCardContainer(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
