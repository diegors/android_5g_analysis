package com.example.signalchecker.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreen]. */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainScreen_titleIsDisplayed() {
        composeTestRule.setContent { MainScreen() }
        composeTestRule.onNodeWithText("Cellular Signal Checker").assertIsDisplayed()
    }

    @Test
    fun mainScreen_captureSignalButtonIsDisplayed() {
        composeTestRule.setContent { MainScreen() }
        composeTestRule.onNodeWithText("Capture Signal").assertIsDisplayed()
    }

    @Test
    fun mainScreen_startMonitoringButtonIsDisplayed() {
        composeTestRule.setContent { MainScreen() }
        composeTestRule.onNodeWithText("Start Background Monitoring").assertIsDisplayed()
    }
}
