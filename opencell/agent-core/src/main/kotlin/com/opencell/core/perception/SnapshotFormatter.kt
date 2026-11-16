package com.opencell.core.perception

import com.opencell.core.model.UiSnapshot
import com.opencell.core.security.Redactor

object SnapshotFormatter {
    fun forModel(snapshot: UiSnapshot, maxNodes: Int = 80): String = buildString {
        appendLine("package=${snapshot.packageName} screen=${snapshot.screenWidth}x${snapshot.screenHeight} keyboard=${snapshot.keyboardVisible} dialog=${snapshot.dialogVisible}")
        snapshot.nodes
            .filter { it.visible && (it.label.isNotBlank() || it.clickable || it.editable || it.scrollable) }
            .take(maxNodes)
            .forEach { n ->
                val flags = listOfNotNull(
                    "clickable".takeIf { n.clickable }, "editable".takeIf { n.editable },
                    "scrollable".takeIf { n.scrollable }, "disabled".takeIf { !n.enabled }, "password".takeIf { n.password },
                ).joinToString(",")
                val label = if (n.password) "" else Redactor.redact(n.label).take(120)
                appendLine("- \"$label\" [$flags] id=${n.viewId ?: "-"}")
            }
    }
}
