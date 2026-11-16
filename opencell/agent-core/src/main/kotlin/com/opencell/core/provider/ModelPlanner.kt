package com.opencell.core.provider

import com.opencell.core.engine.PlanResult
import com.opencell.core.engine.Planner
import com.opencell.core.engine.ReplanContext
import com.opencell.core.model.UiSnapshot

class ModelPlanner(private val provider: LanguageModelProvider) : Planner {

    override suspend fun plan(goal: String, snapshot: UiSnapshot?) = ask(PlanPrompts.planRequest(goal, snapshot))

    override suspend fun replan(context: ReplanContext) = ask(PlanPrompts.replanRequest(context))

    private suspend fun ask(request: ModelRequest): PlanResult {
        var req = request
        repeat(2) { attempt ->
            val text = try {
                provider.generate(req).text
            } catch (e: ProviderException) {
                return PlanResult.Failed(e.message.orEmpty())
            }
            val parsed = PlanParser.parse(text)
            parsed.getOrNull()?.let { return PlanResult.Ok(it) }
            if (attempt == 1) return PlanResult.Failed("Resposta inválida do modelo: ${parsed.exceptionOrNull()?.message}")
            req = req.copy(
                messages = req.messages + ModelMessage(ModelMessage.Role.ASSISTANT, text.take(1500)) +
                    ModelMessage(ModelMessage.Role.USER, "Resposta rejeitada: ${parsed.exceptionOrNull()?.message}. Responda apenas com JSON válido no formato exigido."),
            )
        }
        return PlanResult.Failed("Resposta inválida do modelo.")
    }
}
