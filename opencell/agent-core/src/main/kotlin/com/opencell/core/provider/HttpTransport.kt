package com.opencell.core.provider

import com.opencell.core.security.Redactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class HttpResponse(val code: Int, val body: String, val retryAfterMs: Long?)

interface HttpTransport {
    suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Long): HttpResponse
    suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): HttpResponse
}

class OkHttpTransport(private val client: OkHttpClient = OkHttpClient()) : HttpTransport {
    override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Long) =
        call(Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.get().build(), timeoutMs)

    override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long) =
        call(
            Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }
                .post(body.toRequestBody("application/json".toMediaType())).build(),
            timeoutMs,
        )

    private suspend fun call(request: Request, timeoutMs: Long): HttpResponse = withContext(Dispatchers.IO) {
        val c = client.newBuilder()
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
        try {
            c.newCall(request).execute().use { r ->
                val retry = r.header("Retry-After")?.toLongOrNull()?.times(1000)
                HttpResponse(r.code, r.body?.string().orEmpty(), retry)
            }
        } catch (e: IOException) {
            throw ProviderException.Network(Redactor.redact(e.message ?: e::class.simpleName.orEmpty()))
        }
    }
}
