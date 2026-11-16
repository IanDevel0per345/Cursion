package com.opencell.app.agent

import com.opencell.app.data.AppSettings
import com.opencell.app.device.AppResolver
import com.opencell.core.engine.PlanResult
import com.opencell.core.engine.Planner
import com.opencell.core.engine.ReplanContext
import com.opencell.core.model.UiSnapshot
import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Expectation
import com.opencell.core.protocol.Plan
import com.opencell.core.provider.FallbackProvider
import com.opencell.core.provider.HybridPlanner
import com.opencell.core.provider.LanguageModelProvider
import com.opencell.core.provider.ModelPlanner
import com.opencell.core.provider.ModelRequest
import com.opencell.core.provider.ModelResponse
import com.opencell.core.provider.OkHttpTransport
import com.opencell.core.provider.ProviderConfig
import com.opencell.core.provider.ProviderFactory
import com.opencell.core.provider.RetryingProvider
import com.opencell.core.provider.RuleBasedPlanner

class TokenCounter {
    @Volatile var input = 0
        private set
    @Volatile var output = 0
        private set
    fun reset() { input = 0; output = 0 }
    @Synchronized fun add(i: Int?, o: Int?) { input += i ?: 0; output += o ?: 0 }
}

private class MeteredProvider(private val delegate: LanguageModelProvider, private val counter: TokenCounter) : LanguageModelProvider by delegate {
    override suspend fun generate(request: ModelRequest): ModelResponse =
        delegate.generate(request).also { counter.add(it.inputTokens, it.outputTokens) }
}

private class AppAwarePlanner(private val inner: Planner, private val apps: AppResolver) : Planner {
    override suspend fun plan(goal: String, snapshot: UiSnapshot?) = resolveApps(inner.plan(goal, snapshot))
    override suspend fun replan(context: ReplanContext) = resolveApps(inner.replan(context))

    private fun resolveApps(result: PlanResult): PlanResult {
        val plan = (result as? PlanResult.Ok)?.plan ?: return result
        val steps = plan.steps.map { step ->
            val a = step.action as? AgentAction.OpenApplication ?: return@map step
            val app = apps.resolve(a.appName, a.packageName)
                ?: return PlanResult.Failed(
                    if (a.appName.orEmpty().lowercase().let { "teste" in it || "playground" in it })
                        "O OpenCell Test Playground não está instalado. Instale o APK do Playground para fazer o teste guiado."
                    else "Aplicativo \"${a.appName ?: a.packageName}\" não está instalado ou não tem tela inicial.",
                )
            step.copy(
                description = "Abrir ${app.label}",
                action = AgentAction.OpenApplication(app.label, app.packageName),
                expect = step.expect ?: Expectation.PackageIs(app.packageName),
            )
        }
        return PlanResult.Ok(Plan(plan.version, plan.goal, steps))
    }
}

object PlannerFactory {
    private val http = OkHttpTransport()

    fun create(s: AppSettings, apiKey: String?, apps: AppResolver, counter: TokenCounter): Planner {
        val model = if (!apiKey.isNullOrBlank() && s.model.isNotBlank()) {
            val cfg = ProviderConfig(s.providerKind, s.baseUrl, apiKey, s.model, s.timeoutSec * 1000L, s.temperature.toDoubleOrNull(),
                thinkingBudget = if ("2.5-flash" in s.model) 0 else null)
            val primary = RetryingProvider(ProviderFactory.create(cfg, http), s.providerRetries)
            val fallback = s.fallbackModel.takeIf { it.isNotBlank() }?.let { RetryingProvider(ProviderFactory.create(cfg.copy(model = it), http), s.providerRetries) }
            ModelPlanner(MeteredProvider(FallbackProvider(primary, fallback), counter))
        } else null
        return AppAwarePlanner(HybridPlanner(RuleBasedPlanner(), model), apps)
    }
}
