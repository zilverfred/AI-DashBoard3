package ir.dashboard112.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.dashboard112.app.data.local.entity.TaskEntity
import ir.dashboard112.app.data.local.entity.TaskPriority
import ir.dashboard112.app.ui.screens.DashboardHomeScreen
import ir.dashboard112.app.ui.theme.Dashboard112Theme
import ir.dashboard112.app.ui.viewmodel.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardHomeScreen_rendersAllEightCardsAndHandlesNavigationClick() {
        var navigatedRoute = ""
        val sampleState = DashboardUiState(
            todayIso = "2026-10-10",
            tasks = listOf(
                TaskEntity(
                    id = 10L,
                    title = "بررسی قرارداد بیمه خودرو",
                    priority = TaskPriority.CRITICAL,
                    dateIso = "2026-10-10",
                    isCompleted = false
                )
            )
        )

        composeTestRule.setContent {
            Dashboard112Theme {
                DashboardHomeScreen(
                    uiState = sampleState,
                    onNavigateToRoute = { navigatedRoute = it },
                    onToggleTask = {}
                )
            }
        }

        composeTestRule.onNodeWithText("۱. نمای کلی امروز").assertIsDisplayed()
        composeTestRule.onNodeWithText("۲. کارهای امروز").assertIsDisplayed()
        composeTestRule.onNodeWithText("۳. ورزش و سلامت").assertIsDisplayed()
        composeTestRule.onNodeWithText("۴. خلاصه مالی").assertIsDisplayed()
        composeTestRule.onNodeWithText("۲. کارهای امروز").performClick()

        assertEquals("tasks", navigatedRoute)
    }
}
