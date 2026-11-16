package com.opencell.core.provider

import com.opencell.core.security.Redactor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private val lenient = Json { ignoreUnknownKeys = true }

private fun HttpResponse.ensureOk(): String {
    if (code in 200..299) return body
    val detail = errorDetail(body)
    throw when (code) {
        401, 403 -> ProviderException.Auth("Credencial recusada pelo provedor (HTTP $code). $detail")
        429 -> ProviderException.RateLimited(retryAfterMs, "Limite de requisições atingido (HTTP 429). $detail")
        in 500..599 -> ProviderException.Server(code, "Erro no servidor do provedor (HTTP $code). $detail")
        else -> ProviderException.Client(code, "Requisição recusada (HTTP $code). $detail")
    }
}

private fun errorDetail(body: String): String = runCatching {
    val root = lenient.parseToJsonElement(body).jsonObject
    val err = root["error"]
    val msg = (err as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull ?: (err as? JsonPrimitive)?.contentOrNull
    Redactor.redact(msg.orEmpty()).take(300)
}.getOrDefault("")

private fun parse(body: String): JsonObject = try {
    lenient.parseToJsonElement(body).jsonObject
} catch (e: Exception) {
    throw ProviderException.InvalidResponse("Resposta do provedor não é JSON válido.")
}

private fun String.trimBase() = trimEnd('/')

class OpenAiCompatibleProvider(override val config: ProviderConfig, private val http: HttpTransport) : LanguageModelProvider {
    private val headers get() = mapOf("Authorization" to "Bearer ${config.apiKey}")

    override suspend fun generate(request: ModelRequest): ModelResponse {
        val body = buildJsonObject {
            put("model", config.model)
            put("max_tokens", config.maxTokens)
            config.temperature?.let { put("temperature", it) }
            if (request.jsonMode) putJsonObject("response_format") { put("type", "json_object") }
            putJsonArray("messages") {
                request.system?.let { add(buildJsonObject { put("role", "system"); put("content", it) }) }
                request.messages.forEach { add(buildJsonObject { put("role", it.role.name.lowercase()); put("content", it.content) }) }
            }
        }.toString()
        val root = parse(http.postJson("${config.baseUrl.trimBase()}/chat/completions", headers, body, config.timeoutMs).ensureOk())
        val text = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            ?: throw ProviderException.InvalidResponse("Resposta sem choices[0].message.content.")
        val usage = root["usage"] as? JsonObject
        return ModelResponse(text, usage?.get("prompt_tokens")?.jsonPrimitive?.intOrNull, usage?.get("completion_tokens")?.jsonPrimitive?.intOrNull, config.model)
    }

    override suspend fun listModels(): List<String> {
        val root = parse(http.get("${config.baseUrl.trimBase()}/models", headers, config.timeoutMs).ensureOk())
        return root["data"]?.jsonArray?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.orEmpty().sorted()
    }

    override fun capabilities() = ProviderCapabilities(streaming = false, vision = false, structuredOutput = true)
}

class AnthropicProvider(override val config: ProviderConfig, private val http: HttpTransport) : LanguageModelProvider {
    private val headers get() = mapOf("x-api-key" to config.apiKey, "anthropic-version" to "2023-06-01")

    override suspend fun generate(request: ModelRequest): ModelResponse {
        val body = buildJsonObject {
            put("model", config.model)
            put("max_tokens", config.maxTokens)
            config.temperature?.let { put("temperature", it) }
            request.system?.let { put("system", it) }
            putJsonArray("messages") {
                request.messages.forEach { add(buildJsonObject { put("role", it.role.name.lowercase()); put("content", it.content) }) }
            }
        }.toString()
        val root = parse(http.postJson("${config.baseUrl.trimBase()}/v1/messages", headers, body, config.timeoutMs).ensureOk())
        val text = root["content"]?.jsonArray?.mapNotNull { block ->
            block.jsonObject.takeIf { it["type"]?.jsonPrimitive?.contentOrNull == "text" }?.get("text")?.jsonPrimitive?.contentOrNull
        }?.joinToString("")?.takeIf { it.isNotEmpty() } ?: throw ProviderException.InvalidResponse("Resposta sem bloco de texto.")
        val usage = root["usage"] as? JsonObject
        return ModelResponse(text, usage?.get("input_tokens")?.jsonPrimitive?.intOrNull, usage?.get("output_tokens")?.jsonPrimitive?.intOrNull, config.model)
    }

    override suspend fun listModels(): List<String> {
        val root = parse(http.get("${config.baseUrl.trimBase()}/v1/models", headers, config.timeoutMs).ensureOk())
        return root["data"]?.jsonArray?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.orEmpty()
    }

    override fun capabilities() = ProviderCapabilities(streaming = false, vision = true, structuredOutput = false)
}

class GeminiProvider(override val config: ProviderConfig, private val http: HttpTransport) : LanguageModelProvider {
    private val headers get() = mapOf("x-goog-api-key" to config.apiKey)

    override suspend fun generate(request: ModelRequest): ModelResponse {
        val body = buildJsonObject {
            request.system?.let { putJsonObject("systemInstruction") { putJsonArray("parts") { add(buildJsonObject { put("text", it) }) } } }
            putJsonArray("contents") {
                request.messages.forEach { m ->
                    add(buildJsonObject {
                        put("role", if (m.role == ModelMessage.Role.USER) "user" else "model")
                        putJsonArray("parts") { add(buildJsonObject { put("text", m.content) }) }
                    })
                }
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", config.maxTokens)
                config.temperature?.let { put("temperature", it) }
                if (request.jsonMode) put("responseMimeType", "application/json")
            }
        }.toString()
        val url = "${config.baseUrl.trimBase()}/v1beta/models/${config.model}:generateContent"
        val root = parse(http.postJson(url, headers, body, config.timeoutMs).ensureOk())
        val parts: JsonArray = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?: throw ProviderException.InvalidResponse("Resposta sem candidates[0].content.parts (possível bloqueio de segurança).")
        val text = parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }.joinToString("")
        val usage = root["usageMetadata"] as? JsonObject
        return ModelResponse(text, usage?.get("promptTokenCount")?.jsonPrimitive?.intOrNull, usage?.get("candidatesTokenCount")?.jsonPrimitive?.intOrNull, config.model)
    }

    override suspend fun listModels(): List<String> {
        val root = parse(http.get("${config.baseUrl.trimBase()}/v1beta/models", headers, config.timeoutMs).ensureOk())
        return root["models"]?.jsonArray?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.removePrefix("models/") }.orEmpty()
    }

    override fun capabilities() = ProviderCapabilities(streaming = false, vision = true, structuredOutput = true)
}

object ProviderFactory {
    fun create(config: ProviderConfig, http: HttpTransport): LanguageModelProvider = when (config.kind) {
        ProviderKind.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(config, http)
        ProviderKind.ANTHROPIC -> AnthropicProvider(config, http)
        ProviderKind.GEMINI -> GeminiProvider(config, http)
    }
}
