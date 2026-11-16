package com.opencell.core.provider

import com.opencell.core.engine.PlanResult
import com.opencell.core.engine.Planner
import com.opencell.core.model.UiSnapshot
import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Direction
import com.opencell.core.protocol.Expectation
import com.opencell.core.protocol.Plan
import com.opencell.core.protocol.PlanStep
import com.opencell.core.protocol.Role
import com.opencell.core.protocol.Selector

class RuleBasedPlanner : Planner {

    override suspend fun plan(goal: String, snapshot: UiSnapshot?): PlanResult {
        val quoted = mutableListOf<String>()
        val masked = Regex("[\"“”']([^\"“”']+)[\"“”']").replace(goal) { m ->
            quoted += m.groupValues[1]
            "§${quoted.lastIndex}§"
        }
        val clauses = masked.split(Regex("\\s*(?:;|,| e depois | depois | e então | então | e em seguida | em seguida | e (?=(?:digite|escreva|toque|clique|selecione|escolha|abra|role|volte|entre)\\b))\\s*"))
            .map { it.trim().trimEnd('.') }.filter { it.isNotEmpty() }
        if (clauses.isEmpty()) return PlanResult.Failed("Comando vazio.")

        val steps = mutableListOf<PlanStep>()
        for (clause in clauses) {
            val action = parseClause(clause.replace(Regex("§(\\d+)§")) { quoted[it.groupValues[1].toInt()] }, clause, quoted)
                ?: return PlanResult.Failed("Sem um modelo de IA configurado, não entendi: \"$clause\". Configure um provedor para comandos livres.")
            steps += PlanStep("s${steps.size + 1}", action.second, action.first, action.third)
        }
        steps += PlanStep("s${steps.size + 1}", "Concluir", AgentAction.CompleteTask("Plano executado."))
        return PlanResult.Ok(Plan(goal = goal, steps = steps))
    }

    private fun unquote(raw: String, quoted: List<String>) =
        Regex("§(\\d+)§").replace(raw) { quoted[it.groupValues[1].toInt()] }.trim()

    private val verbs = Regex("\\b(pesquis\\w*|busc\\w*|procur\\w*|cri\\w+|ativ\\w+|desativ\\w+|digit\\w+|escrev\\w+|envi\\w+|entre|entra|abr\\w+|toqu\\w+|cliqu\\w+|selecion\\w+|role|rol\\w+|volt\\w+|e|para|com|que|depois|sem)\\b")
    private fun simpleLabel(label: String, maxWords: Int) =
        label.isNotBlank() && label.trim().split(Regex("\\s+")).size <= maxWords && !verbs.containsMatchIn(label.lowercase())

    private fun parseClause(@Suppress("UNUSED_PARAMETER") plain: String, clause: String, quoted: List<String>): Triple<AgentAction, String, Expectation?>? {
        val c = clause.lowercase()
        Regex("^(?:toque|toca|clique|clica|selecione|seleciona|escolha|abra a aba|entre na aba|entre em)\\s+(?:em\\s+|na\\s+|no\\s+|a\\s+|o\\s+)?(?:aba\\s+|botão\\s+|conversa\\s+)?(.+)$").find(c)?.let {
            val label = unquote(clause.substring(it.groups[1]!!.range.first), quoted)
            if (!simpleLabel(label, 4) && !clause.contains("§")) return null
            return Triple(AgentAction.TapElement(Selector(text = label)), "Tocar em \"$label\"", null)
        }
        Regex("^(?:abra|abrir|abre|inicie)\\s+(?:o\\s+|a\\s+)?(?:app|aplicativo|aplicação)?\\s*(?:de\\s+|do\\s+|da\\s+)?(.+)$").find(c)?.let {
            val name = unquote(clause.substring(it.groups[1]!!.range.first), quoted)
            if (!simpleLabel(name, 3)) return null
            return Triple(AgentAction.OpenApplication(appName = name), "Abrir aplicativo $name", null)
        }
        Regex("^(?:digite|digita|escreva|escreve)\\s+(.+?)\\s+(?:no|na)\\s+(?:campo|caixa)(?:\\s+(?:de\\s+)?(.+))?$").find(c)?.let {
            val text = unquote(clause.substring(it.groups[1]!!.range), quoted)
            val hint = it.groups[2]?.let { g -> unquote(clause.substring(g.range), quoted) }
            val selector = Selector(role = Role.INPUT, textContains = null, description = null).let { s ->
                if (hint != null) s.copy(textContains = hint) else s
            }
            return Triple(AgentAction.EnterText(selector, text), "Digitar \"$text\" no campo", Expectation.FieldContains(Selector(role = Role.INPUT), text))
        }
        Regex("^(?:role|rola|rolar|desça|desce|suba|sobe)\\s*(?:a tela\\s*)?(?:para\\s+)?(baixo|cima)?").find(c)?.let {
            val dir = if (it.groupValues[1] == "cima" || c.startsWith("suba") || c.startsWith("sobe")) Direction.UP else Direction.DOWN
            return Triple(AgentAction.ScrollContainer(dir), "Rolar para ${if (dir == Direction.UP) "cima" else "baixo"}", Expectation.Scrolled)
        }
        if (c.startsWith("volte") || c == "voltar" || c.startsWith("pressione voltar")) {
            return Triple(AgentAction.PressBack, "Voltar", null)
        }
        return null
    }
}

class HybridPlanner(private val deterministic: Planner, private val model: Planner?) : Planner {
    override suspend fun plan(goal: String, snapshot: UiSnapshot?): PlanResult {
        val local = deterministic.plan(goal, snapshot)
        if (local is PlanResult.Ok || model == null) return local
        return model.plan(goal, snapshot)
    }

    override suspend fun replan(context: com.opencell.core.engine.ReplanContext): PlanResult =
        model?.replan(context) ?: PlanResult.Failed("Replanejamento exige um provedor de IA configurado.")
}
