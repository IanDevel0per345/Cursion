package com.opencell.core.perception

import com.opencell.core.model.UiSnapshot
import com.opencell.core.protocol.Expectation

data class Verification(val ok: Boolean, val detail: String)

object Verifier {

    fun check(expectation: Expectation, before: UiSnapshot?, after: UiSnapshot): Verification = when (expectation) {
        is Expectation.TextVisible ->
            result(after.nodes.any { it.visible && it.label.contains(expectation.text, true) }, "texto \"${expectation.text}\" visível")
        is Expectation.TextAbsent ->
            result(after.nodes.none { it.visible && it.label.contains(expectation.text, true) }, "texto \"${expectation.text}\" ausente")
        is Expectation.ElementExists ->
            result(ElementResolver.resolve(expectation.selector, after) !is Resolution.NotFound, "elemento presente")
        is Expectation.FieldContains -> {
            val res = ElementResolver.resolve(expectation.selector, after)
            val node = (res as? Resolution.Found)?.node ?: (res as? Resolution.Ambiguous)?.candidates?.firstOrNull { it.focused }
            result(node?.text?.contains(expectation.text, true) == true, "campo contém \"${expectation.text}\"")
        }
        is Expectation.PackageIs -> result(after.packageName == expectation.packageName, "app em primeiro plano é ${expectation.packageName}")
        Expectation.ScreenChanged -> result(before == null || signature(before) != signature(after), "tela mudou")
        Expectation.Scrolled -> result(before == null || contentSignature(before) != contentSignature(after), "conteúdo rolou")
    }

    fun signature(s: UiSnapshot): Int =
        (s.packageName + "|" + s.nodes.joinToString("|") { "${it.label}@${it.bounds}" }).hashCode()

    private fun contentSignature(s: UiSnapshot): Int =
        s.nodes.filter { it.label.isNotBlank() }.map { it.label }.toSet().hashCode()

    private fun result(ok: Boolean, what: String) = Verification(ok, if (ok) "Confirmado: $what" else "Não confirmado: $what")
}
