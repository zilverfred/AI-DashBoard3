package ir.dashboard112.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.dashboard112.app.data.local.entity.TaskPriority
import ir.dashboard112.app.data.local.entity.TransactionType
import ir.dashboard112.app.data.local.entity.VehicleUrgency
import ir.dashboard112.app.data.preferences.ThemeModePreference
import ir.dashboard112.app.domain.util.FinanceCalculator
import ir.dashboard112.app.domain.util.SleepCalculator
import ir.dashboard112.app.ui.theme.AppAccentPalette
import ir.dashboard112.app.ui.viewmodel.DashboardUiState
import ir.dashboard112.app.ui.viewmodel.DashboardViewModel

@Composable
fun TasksAndOfficeModuleScreen(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.IMPORTANT) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("مدیریت کارهای روزانه و وظایف اداری", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("جستجو در کارها...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان کار جدید") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { item ->
                    TextButton(onClick = { priority = item }) {
                        Text(item.labelFa)
                    }
                }
            }
            Button(
                onClick = {
                    viewModel.saveTask(
                        title = title,
                        note = note,
                        priority = priority,
                        dateIso = uiState.todayIso
                    )
                    title = ""
                    note = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("افزودن کار")
            }
        }

        items(uiState.tasks, key = { it.id }) { task ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${task.title} · ${task.priority.labelFa}")
                        if (task.note.isNotBlank()) Text(task.note)
                    }
                    TextButton(onClick = { viewModel.deleteTask(task) }) {
                        Text("حذف")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutAndSleepModuleScreen(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    var workoutType by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("45") }
    var weightText by remember { mutableStateOf("75.0") }
    var recordText by remember { mutableStateOf("") }

    var sleepStart by remember { mutableStateOf("23:15") }
    var wakeUp by remember { mutableStateOf("06:45") }
    var restMinutes by remember { mutableStateOf("60") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("ورزش و خواب و استراحت", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = workoutType,
                onValueChange = { workoutType = it },
                label = { Text("نوع تمرین (مثلاً دویدن یا بدنسازی)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.saveWorkout(
                        workoutType = workoutType,
                        durationMinutes = durationText.toIntOrNull() ?: 0,
                        bodyWeightKg = weightText.toDoubleOrNull() ?: 0.0,
                        personalRecordText = recordText,
                        targetProgressPercent = 80,
                        dateIso = uiState.todayIso
                    )
                    workoutType = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ثبت جلسه تمرینی")
            }
        }

        item {
            Text("ثبت خواب و استراحت (پشتیبانی از عبور از نیمه‌شب)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sleepStart,
                    onValueChange = { sleepStart = it },
                    label = { Text("ساعت خواب (HH:mm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = wakeUp,
                    onValueChange = { wakeUp = it },
                    label = { Text("ساعت بیداری (HH:mm)") },
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    viewModel.saveSleepRecord(
                        dateIso = uiState.todayIso,
                        sleepStartHm = sleepStart,
                        wakeUpHm = wakeUp,
                        restAndLeisureMinutes = restMinutes.toIntOrNull() ?: 0,
                        qualityRating = 4,
                        notes = ""
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ثبت رکورد خواب")
            }
        }

        items(uiState.sleepRecords, key = { it.id }) { sleep ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("تاریخ: ${sleep.dateIso} · مدت خواب: ${SleepCalculator.formatMinutesFa(sleep.sleepDurationMinutes)}")
                    Text("از ${sleep.sleepStartHm} تا ${sleep.wakeUpHm} · استراحت: ${sleep.restAndLeisureMinutes} دقیقه")
                }
            }
        }
    }
}

@Composable
fun FinanceModuleScreen(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("خوراک و روزمره") }
    var txType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val summary = FinanceCalculator.calculateSummary(
        transactions = uiState.financeTransactions,
        todayIso = uiState.todayIso,
        selectedJalaliYearMonth = uiState.selectedJalaliMonth
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("مدیریت مالی (${uiState.preferences.currencyUnit})", style = MaterialTheme.typography.headlineMedium)
            Text("مانده کل: ${summary.netBalance} · درآمد ماه: ${summary.selectedMonthIncome} · هزینه ماه: ${summary.selectedMonthExpense}")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان تراکنش") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("مبلغ (${uiState.preferences.currencyUnit})") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { item ->
                    TextButton(onClick = { txType = item }) { Text(item.labelFa) }
                }
            }
            Button(
                onClick = {
                    viewModel.saveFinanceTransaction(
                        title = title,
                        amountTomans = amountText.toLongOrNull() ?: -1L,
                        type = txType,
                        category = category,
                        dateIso = uiState.todayIso,
                        description = ""
                    )
                    title = ""
                    amountText = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ثبت تراکنش مالی")
            }
        }

        items(uiState.financeTransactions, key = { it.id }) { tx ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${tx.title} (${tx.category})")
                    Text("${tx.amountTomans} ${uiState.preferences.currencyUnit} · ${tx.type.labelFa}")
                }
            }
        }
    }
}

@Composable
fun VehicleModuleScreen(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    var vehicleName by remember { mutableStateOf("پژو ۲۰۷") }
    var serviceTitle by remember { mutableStateOf("") }
    var currentKm by remember { mutableStateOf("64500") }
    var dueKm by remember { mutableStateOf("65000") }
    var costText by remember { mutableStateOf("1850000") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("مدیریت سرویس و تعمیرات خودرو", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = serviceTitle,
                onValueChange = { serviceTitle = it },
                label = { Text("عنوان سرویس یا تعمیر (مثلاً تعویض روغن موتور)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.saveVehicleService(
                        vehicleName = vehicleName,
                        serviceTitle = serviceTitle,
                        urgency = VehicleUrgency.UPCOMING_SERVICE,
                        currentOdometerKm = currentKm.toIntOrNull() ?: 0,
                        dueOdometerKm = dueKm.toIntOrNull() ?: 0,
                        costTomans = costText.toLongOrNull() ?: 0L,
                        dueDateIso = uiState.todayIso,
                        isResolved = false,
                        notes = ""
                    )
                    serviceTitle = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ثبت سرویس خودرو و همگام‌سازی هزینه")
            }
        }

        items(uiState.vehicleServices, key = { it.id }) { service ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${service.vehicleName} — ${service.serviceTitle}")
                    Text("کیلومتر فعلی: ${service.currentOdometerKm} · سررسید: ${service.dueOdometerKm} کیلومتر")
                    Text("هزینه: ${service.costTomans} ${uiState.preferences.currencyUnit}")
                }
            }
        }
    }
}

@Composable
fun ReportsModuleScreen(uiState: DashboardUiState) {
    val summary = FinanceCalculator.calculateSummary(
        transactions = uiState.financeTransactions,
        todayIso = uiState.todayIso,
        selectedJalaliYearMonth = uiState.selectedJalaliMonth
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("گزارش‌های آماری هفتگی و ماهانه", style = MaterialTheme.typography.headlineMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تعداد کل کارهای ثبت‌شده: ${uiState.tasks.size}")
                    Text("تعداد جلسات ورزشی: ${uiState.workouts.size}")
                    Text("جمع درآمد کل: ${summary.totalIncome} ${uiState.preferences.currencyUnit}")
                    Text("جمع هزینه کل: ${summary.totalExpense} ${uiState.preferences.currencyUnit}")
                    Text("مانده خالص: ${summary.netBalance} ${uiState.preferences.currencyUnit}")
                }
            }
        }
    }
}

@Composable
fun SettingsModuleScreen(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("تنظیمات، پشتیبان‌گیری و درباره برنامه", style = MaterialTheme.typography.headlineMedium)
            Text("انتخاب حالت نمایش (روشن / تاریک / سیستم):")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModePreference.entries.forEach { mode ->
                    TextButton(onClick = { viewModel.updateThemeMode(mode) }) {
                        Text(mode.labelFa)
                    }
                }
            }
            Text("انتخاب رنگ اصلی برنامه:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppAccentPalette.entries.forEach { palette ->
                    TextButton(onClick = { viewModel.updatePrimaryColor(palette.key) }) {
                        Text(palette.labelFa)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("درباره Dashboard 112 — نسخه ۱.۲.۰ (Build 2)")
                    Text("معماری: MVVM + Jetpack Compose + Room Database + DataStore + AlarmManager")
                    Text("کاملاً آفلاین با حفظ حریم خصوصی در حافظه داخلی دستگاه")
                }
            }
        }
    }
}
