package com.opencell.core.provider

import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Plan
import kotlinx.serialization.json.Json

object PlanParser {
    private const val MAX_STEPS = 30
    private const val MAX_TEXT = 2000

    private val strict = Json { ignoreUnknownKeys = false; isLenient = false; classDiscriminator = "type" }

    fun parse(raw: String): Result<Plan> = runCatching {
        val json = extractJson(raw) ?: error("Nenhum objeto JSON encontrado na resposta do modelo.")
        val plan = strict.decodeFromString(Plan.serializer(), json)
        validate(plan)
        plan
    }

    private fun validate(plan: Plan) {
        require(plan.steps.isNotEmpty()) { "Plano sem passos." }
        require(plan.steps.size <= MAX_STEPS) { "Plano excede $MAX_STEPS passos." }
        require(plan.steps.map { it.id }.toSet().size == plan.steps.size) { "IDs de passos duplicados." }
        plan.steps.forEach { step ->
            when (val a = step.action) {
                is AgentAction.EnterText -> require(a.text.length <= MAX_TEXT) { "Texto de digitação longo demais." }
                is AgentAction.TapPoint -> require(a.x in 0f..1f && a.y in 0f..1f) { "Coordenadas fora de 0..1." }
                is AgentAction.OpenApplication -> require(!a.appName.isNullOrBlank() || !a.packageName.isNullOrBlank()) { "open_application sem alvo." }
                else -> Unit
            }
        }
    }

    private fun extractJson(raw: String): String? {
        val fenced = Regex("```(?:json)?\\s*(\\{.*?})\\s*```", RegexOption.DOT_MATCHES_ALL).find(raw)?.groupValues?.get(1)
        if (fenced != null) return fenced
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else null
    }
}
