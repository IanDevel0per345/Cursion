package com.opencell.core.perception

import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.protocol.Role
import com.opencell.core.protocol.Selector

sealed interface Resolution {
    data class Found(val node: UiNode) : Resolution
    data object NotFound : Resolution
    data class Ambiguous(val candidates: List<UiNode>) : Resolution
}

object ElementResolver {

    fun resolve(selector: Selector, snapshot: UiSnapshot): Resolution {
        if (selector.isEmpty) return Resolution.NotFound
        val visible = snapshot.nodes.filter { it.visible && !it.bounds.isEmpty }
        val scored = visible.mapNotNull { node -> score(selector, node)?.let { node to it } }
        if (scored.isEmpty()) return Resolution.NotFound
        val best = scored.maxOf { it.second }
        val top = scored.filter { it.second == best }.map { it.first }
        val deduped = dedupeNested(top).sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
        selector.index?.let { i -> return deduped.getOrNull(i)?.let { Resolution.Found(it) } ?: Resolution.NotFound }
        return if (deduped.size == 1) Resolution.Found(deduped.first()) else Resolution.Ambiguous(deduped)
    }

    private fun score(selector: Selector, node: UiNode): Int? {
        var score = 0
        selector.text?.let { wanted ->
            score += when {
                node.text.equalsLoose(wanted) || node.description.equalsLoose(wanted) -> 3
                else -> return null
            }
        }
        selector.textContains?.let { wanted ->
            if (!node.label.contains(wanted, ignoreCase = true)) return null
            score += 2
        }
        selector.description?.let { wanted ->
            if (!node.description.equalsLoose(wanted)) return null
            score += 3
        }
        selector.viewId?.let { wanted ->
            val id = node.viewId ?: return null
            if (id != wanted && !id.endsWith("/$wanted")) return null
            score += 4
        }
        selector.role?.let { role -> if (!matchesRole(role, node)) return null else score += 1 }
        return score
    }

    private fun matchesRole(role: Role, node: UiNode) = when (role) {
        Role.BUTTON -> node.clickable || node.className?.contains("Button") == true
        Role.INPUT -> node.editable
        Role.LIST -> node.scrollable
        Role.ANY -> true
    }

    private fun String?.equalsLoose(other: String) = this != null && this.trim().equals(other.trim(), ignoreCase = true)

    private fun dedupeNested(nodes: List<UiNode>): List<UiNode> =
        nodes.groupBy { it.bounds }.values.map { group -> group.firstOrNull { it.clickable || it.editable } ?: group.first() }
}
