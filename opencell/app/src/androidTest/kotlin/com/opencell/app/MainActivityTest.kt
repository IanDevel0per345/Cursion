package com.opencell.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun launchesWithoutCrashAndShowsOnboardingOrHome() {
        val onboarding = runCatching { rule.onNodeWithText("Bem-vindo ao OpenCell").assertExists() }.isSuccess
        if (onboarding) {
            rule.onNodeWithText("Continuar").performClick()
            rule.onNodeWithText("Como funciona").assertExists()
        } else {
            rule.onNodeWithText("Conversar com OpenCell").assertExists()
        }
    }
}
