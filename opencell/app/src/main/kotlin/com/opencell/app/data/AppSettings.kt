package com.opencell.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opencell.core.policy.AutonomyMode
import com.opencell.core.provider.ProviderConfig
import com.opencell.core.provider.ProviderKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboardingDone: Boolean = false,
    val providerKind: ProviderKind = ProviderKind.OPENAI_COMPATIBLE,
    val baseUrl: String = ProviderConfig.defaultBaseUrl(ProviderKind.OPENAI_COMPATIBLE),
    val model: String = "",
    val fallbackModel: String = "",
    val timeoutSec: Int = 60,
    val providerRetries: Int = 3,
    val temperature: String = "",
    val autonomy: AutonomyMode = AutonomyMode.SUPERVISED,
    val maxSteps: Int = 40,
    val taskTimeoutSec: Int = 300,
    val maxRetriesPerAction: Int = 2,
    val blockedPackages: Set<String> = emptySet(),
    val allowedPackages: Set<String> = emptySet(),
    val confirmExternal: Boolean = true,
    val confirmIrreversible: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val retentionDays: Int = 30,
    val speakResponses: Boolean = false,
    val language: String = "pt-BR",
    val wakeWordEnabled: Boolean = false,
)

private val Context.dataStore by preferencesDataStore("opencell_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val kind = stringPreferencesKey("provider_kind")
        val baseUrl = stringPreferencesKey("base_url")
        val model = stringPreferencesKey("model")
        val fallback = stringPreferencesKey("fallback_model")
        val timeout = intPreferencesKey("timeout_sec")
        val retries = intPreferencesKey("provider_retries")
        val temperature = stringPreferencesKey("temperature")
        val autonomy = stringPreferencesKey("autonomy")
        val maxSteps = intPreferencesKey("max_steps")
        val taskTimeout = intPreferencesKey("task_timeout_sec")
        val actionRetries = intPreferencesKey("action_retries")
        val blocked = stringPreferencesKey("blocked_packages")
        val allowed = stringPreferencesKey("allowed_packages")
        val confirmExternal = booleanPreferencesKey("confirm_external")
        val confirmIrreversible = booleanPreferencesKey("confirm_irreversible")
        val theme = stringPreferencesKey("theme")
        val haptics = booleanPreferencesKey("haptics")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val retention = intPreferencesKey("retention_days")
        val speak = booleanPreferencesKey("speak")
        val language = stringPreferencesKey("language")
        val wake = booleanPreferencesKey("wake_word")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[K.onboarding] = next.onboardingDone
            prefs[K.kind] = next.providerKind.name
            prefs[K.baseUrl] = next.baseUrl
            prefs[K.model] = next.model
            prefs[K.fallback] = next.fallbackModel
            prefs[K.timeout] = next.timeoutSec.coerceIn(5, 300)
            prefs[K.retries] = next.providerRetries.coerceIn(0, 5)
            prefs[K.temperature] = next.temperature
            prefs[K.autonomy] = next.autonomy.name
            prefs[K.maxSteps] = next.maxSteps.coerceIn(1, 200)
            prefs[K.taskTimeout] = next.taskTimeoutSec.coerceIn(10, 3600)
            prefs[K.actionRetries] = next.maxRetriesPerAction.coerceIn(0, 5)
            prefs[K.blocked] = next.blockedPackages.joinToString(",")
            prefs[K.allowed] = next.allowedPackages.joinToString(",")
            prefs[K.confirmExternal] = next.confirmExternal
            prefs[K.confirmIrreversible] = next.confirmIrreversible
            prefs[K.theme] = next.theme.name
            prefs[K.haptics] = next.haptics
            prefs[K.reduceMotion] = next.reduceMotion
            prefs[K.retention] = next.retentionDays.coerceIn(0, 365)
            prefs[K.speak] = next.speakResponses
            prefs[K.language] = next.language
            prefs[K.wake] = next.wakeWordEnabled
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        val kind = this[K.kind]?.let { runCatching { ProviderKind.valueOf(it) }.getOrNull() } ?: d.providerKind
        return AppSettings(
            onboardingDone = this[K.onboarding] ?: d.onboardingDone,
            providerKind = kind,
            baseUrl = this[K.baseUrl] ?: ProviderConfig.defaultBaseUrl(kind),
            model = this[K.model] ?: d.model,
            fallbackModel = this[K.fallback] ?: d.fallbackModel,
            timeoutSec = this[K.timeout] ?: d.timeoutSec,
            providerRetries = this[K.retries] ?: d.providerRetries,
            temperature = this[K.temperature] ?: d.temperature,
            autonomy = this[K.autonomy]?.let { runCatching { AutonomyMode.valueOf(it) }.getOrNull() } ?: d.autonomy,
            maxSteps = this[K.maxSteps] ?: d.maxSteps,
            taskTimeoutSec = this[K.taskTimeout] ?: d.taskTimeoutSec,
            maxRetriesPerAction = this[K.actionRetries] ?: d.maxRetriesPerAction,
            blockedPackages = this[K.blocked].toSet(),
            allowedPackages = this[K.allowed].toSet(),
            confirmExternal = this[K.confirmExternal] ?: d.confirmExternal,
            confirmIrreversible = this[K.confirmIrreversible] ?: d.confirmIrreversible,
            theme = this[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
            haptics = this[K.haptics] ?: d.haptics,
            reduceMotion = this[K.reduceMotion] ?: d.reduceMotion,
            retentionDays = this[K.retention] ?: d.retentionDays,
            speakResponses = this[K.speak] ?: d.speakResponses,
            language = this[K.language] ?: d.language,
            wakeWordEnabled = this[K.wake] ?: d.wakeWordEnabled,
        )
    }

    private fun String?.toSet() = this.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}
