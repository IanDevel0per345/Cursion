package com.opencell.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2
    val centerY get() = (top + bottom) / 2
    val isEmpty get() = width <= 0 || height <= 0
}

@Serializable
data class UiNode(
    val id: String,
    val text: String? = null,
    val description: String? = null,
    val viewId: String? = null,
    val className: String? = null,
    val bounds: Bounds,
    val clickable: Boolean = false,
    val longClickable: Boolean = false,
    val editable: Boolean = false,
    val scrollable: Boolean = false,
    val enabled: Boolean = true,
    val focused: Boolean = false,
    val password: Boolean = false,
    val visible: Boolean = true,
    val depth: Int = 0,
) {
    val label: String get() = text?.takeIf { it.isNotBlank() } ?: description.orEmpty()
}

@Serializable
data class UiSnapshot(
    val id: Long,
    val timestampMs: Long,
    val packageName: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val nodes: List<UiNode>,
    val keyboardVisible: Boolean = false,
    val dialogVisible: Boolean = false,
    val protectedContent: Boolean = false,
) {
    fun find(id: String) = nodes.firstOrNull { it.id == id }
}
