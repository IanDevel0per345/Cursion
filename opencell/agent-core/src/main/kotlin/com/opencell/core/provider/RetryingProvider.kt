package com.opencell.core.provider

import kotlinx.coroutines.delay

class RetryingProvider(
    private val delegate: LanguageModelProvider,
    private val maxRetries: Int = 2,
    private val baseDelayMs: Long = 800,
    private val maxDelayMs: Long = 20_000,
) : LanguageModelProvider by delegate {

    override suspend fun generate(request: ModelRequest): ModelResponse {
        var attempt = 0
        while (true) {
            try {
                return delegate.generate(request)
            } catch (e: ProviderException) {
                if (!e.retryable || attempt >= maxRetries) throw e
                val wait = ((e as? ProviderException.RateLimited)?.retryAfterMs ?: (baseDelayMs shl attempt)).coerceAtMost(maxDelayMs)
                delay(wait)
                attempt++
            }
        }
    }
}

class FallbackProvider(private val primary: LanguageModelProvider, private val fallback: LanguageModelProvider?) :
    LanguageModelProvider by primary {
    override suspend fun generate(request: ModelRequest): ModelResponse = try {
        primary.generate(request)
    } catch (e: ProviderException) {
        if (fallback == null || e is ProviderException.Auth) throw e
        fallback.generate(request)
    }
}

data class ConnectionTestResult(val step: String, val ok: Boolean, val detail: String, val latencyMs: Long)

object ConnectionTester {
    suspend fun run(provider: LanguageModelProvider, now: () -> Long = System::currentTimeMillis): List<ConnectionTestResult> {
        val results = mutableListOf<ConnectionTestResult>()
        suspend fun step(name: String, block: suspend () -> String): Boolean {
            val t0 = now()
            return try {
                val detail = block()
                results += ConnectionTestResult(name, true, detail, now() - t0)
                true
            } catch (e: ProviderException) {
                results += ConnectionTestResult(name, false, e.message.orEmpty(), now() - t0)
                false
            }
        }
        step("Conexão e credencial") { "${provider.listModels().size} modelos disponíveis" }
        val textOk = step("Geração de texto") {
            val r = provider.generate(ModelRequest("Responda apenas com a palavra: pronto", listOf(ModelMessage(ModelMessage.Role.USER, "Teste de conexão"))))
            "Resposta recebida (${r.text.take(40).trim()})"
        }
        if (textOk) {
            step("Saída estruturada (plano)") {
                val r = provider.generate(PlanPrompts.structuredProbe())
                PlanParser.parse(r.text).fold({ "Plano JSON válido com ${it.steps.size} passo(s)" }, { throw ProviderException.InvalidResponse(it.message.orEmpty()) })
            }
        }
        return results
    }
}
