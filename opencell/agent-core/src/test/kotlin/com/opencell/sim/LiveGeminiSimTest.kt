package com.opencell.sim

import com.opencell.core.provider.FallbackProvider
import com.opencell.core.provider.GeminiProvider
import com.opencell.core.provider.HybridPlanner
import com.opencell.core.provider.LanguageModelProvider
import com.opencell.core.provider.ModelPlanner
import com.opencell.core.provider.ModelRequest
import com.opencell.core.provider.ModelResponse
import com.opencell.core.provider.OkHttpTransport
import com.opencell.core.provider.ProviderConfig
import com.opencell.core.provider.ProviderException
import com.opencell.core.provider.ProviderKind
import com.opencell.core.provider.RetryingProvider
import com.opencell.core.provider.RuleBasedPlanner
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private class Counting(val d: LanguageModelProvider, val calls: AtomicInteger, val errs: ConcurrentHashMap<String, AtomicInteger>) : LanguageModelProvider by d {
    override suspend fun generate(request: ModelRequest): ModelResponse {
        calls.incrementAndGet()
        try { return d.generate(request) } catch (e: ProviderException) { errs.computeIfAbsent(e::class.simpleName + ":" + d.config.model) { AtomicInteger() }.incrementAndGet(); throw e }
    }
}

class LiveGeminiSimTest {
    @Test fun live() {
        val key = System.getenv("OC_LIVE_KEY"); assumeTrue(key != null)
        val n = System.getenv("OC_SIM_N")?.toInt() ?: 39
        val models = (System.getenv("OC_LIVE_MODELS") ?: "gemini-3.5-flash,gemini-3.6-flash,gemini-3.1-flash-lite,gemini-3-flash-preview").split(",")
        val seed = System.getenv("OC_SIM_SEED")?.toInt() ?: 100
        val calls = AtomicInteger(); val errs = ConcurrentHashMap<String, AtomicInteger>()
        val http = OkHttpTransport()
        fun provider(m: String): LanguageModelProvider =
            RetryingProvider(Counting(GeminiProvider(ProviderConfig(ProviderKind.GEMINI, ProviderConfig.defaultBaseUrl(ProviderKind.GEMINI), key!!, m, 120_000), http), calls, errs), maxRetries = 3, baseDelayMs = 4000, maxDelayMs = 30_000)
        val tasks = SimTasks.generate(n, seed)
        val out = File(System.getenv("OC_SIM_OUT") ?: "/tmp/sim-live.log")
        val results = SimRunner.runAll(tasks, seed, faults = true, concurrency = (System.getenv("OC_SIM_CONC") ?: "3").toInt(), planner = { t, _ ->
            val primary = provider(models[t.id % models.size]); val second = provider(models[(t.id + 1) % models.size])
            HybridPlanner(RuleBasedPlanner(), ModelPlanner(FallbackProvider(primary, second)))
        }, out = out)
        val rep = SimRunner.report(results) + "chamadas à API: ${calls.get()}  erros do provedor: ${errs.entries.associate { it.key to it.value.get() }}\n"
        File(out.path + ".summary").writeText(rep); println(rep)
    }
}
