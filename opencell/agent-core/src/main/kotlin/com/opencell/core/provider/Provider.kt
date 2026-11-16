package com.opencell.core.provider

import kotlinx.coroutines.flow.Flow

enum class ProviderKind { OPENAI_COMPATIBLE, ANTHROPIC, GEMINI }

data class ProviderConfig(
    val kind: ProviderKind,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val timeoutMs: Long = 60_000,
    val temperature: Double? = null,
    val maxTokens: Int = 8192,
    val thinkingBudget: Int? = null,
) {
    companion object {
        fun defaultBaseUrl(kind: ProviderKind) = when (kind) {
            ProviderKind.OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
            ProviderKind.ANTHROPIC -> "https://api.anthropic.com"
            ProviderKind.GEMINI -> "https://generativelanguage.googleapis.com"
        }
    }
}

data class ModelMessage(val role: Role, val content: String) {
    enum class Role { USER, ASSISTANT }
}

data class ModelRequest(
    val system: String?,
    val messages: List<ModelMessage>,
    val jsonMode: Boolean = false,
)

data class ModelResponse(val text: String, val inputTokens: Int?, val outputTokens: Int?, val model: String)

data class ProviderCapabilities(val streaming: Boolean, val vision: Boolean, val structuredOutput: Boolean)

sealed class ProviderException(message: String, val retryable: Boolean) : Exception(message) {
    class Auth(message: String) : ProviderException(message, false)
    class RateLimited(val retryAfterMs: Long?, message: String) : ProviderException(message, true)
    class Server(val code: Int, message: String) : ProviderException(message, true)
    class Client(val code: Int, message: String) : ProviderException(message, false)
    class Network(message: String) : ProviderException(message, true)
    class InvalidResponse(message: String) : ProviderException(message, false)
}

interface LanguageModelProvider {
    val config: ProviderConfig
    suspend fun generate(request: ModelRequest): ModelResponse
    suspend fun listModels(): List<String>
    fun capabilities(): ProviderCapabilities
    fun stream(request: ModelRequest): Flow<String> = kotlinx.coroutines.flow.flow { emit(generate(request).text) }
}
