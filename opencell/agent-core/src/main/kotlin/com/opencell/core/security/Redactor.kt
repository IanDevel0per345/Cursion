package com.opencell.core.security

object Redactor {
    private val patterns = listOf(
        Regex("sk-[A-Za-z0-9_\\-]{16,}") to "[REDACTED_KEY]",
        Regex("sk-ant-[A-Za-z0-9_\\-]{16,}") to "[REDACTED_KEY]",
        Regex("AIza[0-9A-Za-z_\\-]{20,}") to "[REDACTED_KEY]",
        Regex("(?i)bearer\\s+[A-Za-z0-9._\\-]{12,}") to "Bearer [REDACTED]",
        Regex("(?i)(api[_-]?key|x-api-key|token|senha|password)\\s*[=:]\\s*\\S+") to "$1=[REDACTED]",
        Regex("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b") to "[REDACTED_ID]",
        Regex("\\b(?:\\d[ -]?){13,19}\\b") to "[REDACTED_CARD]",
    )

    fun redact(input: String): String = patterns.fold(input) { acc, (re, repl) -> re.replace(acc, repl) }
}
