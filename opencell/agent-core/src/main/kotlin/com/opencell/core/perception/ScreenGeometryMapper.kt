package com.opencell.core.perception

import com.opencell.core.model.UiSnapshot

data class ScreenInsets(val top: Int = 0, val bottom: Int = 0, val left: Int = 0, val right: Int = 0)

data class ScreenState(val width: Int, val height: Int, val insets: ScreenInsets = ScreenInsets(), val nowMs: Long)

sealed interface PointResult {
    data class Ok(val x: Int, val y: Int) : PointResult
    data class Rejected(val reason: String) : PointResult
}

object ScreenGeometryMapper {
    const val MAX_SNAPSHOT_AGE_MS = 5_000L

    fun toPixels(nx: Float, ny: Float, snapshot: UiSnapshot, current: ScreenState): PointResult {
        if (nx !in 0f..1f || ny !in 0f..1f) return PointResult.Rejected("Coordenadas normalizadas fora de 0..1")
        if (snapshot.screenWidth <= 0 || snapshot.screenHeight <= 0) return PointResult.Rejected("Snapshot sem dimensões")
        if (snapshot.screenWidth != current.width || snapshot.screenHeight != current.height) {
            return PointResult.Rejected("Orientação ou resolução mudou desde o snapshot ${snapshot.id}")
        }
        if (current.nowMs - snapshot.timestampMs > MAX_SNAPSHOT_AGE_MS) return PointResult.Rejected("Snapshot desatualizado")
        val x = (nx * current.width).toInt().coerceIn(0, current.width - 1)
        val y = (ny * current.height).toInt().coerceIn(0, current.height - 1)
        val ins = current.insets
        if (y < ins.top || y > current.height - ins.bottom || x < ins.left || x > current.width - ins.right) {
            return PointResult.Rejected("Ponto cai sobre barra do sistema ou recorte de tela")
        }
        return PointResult.Ok(x, y)
    }
}
