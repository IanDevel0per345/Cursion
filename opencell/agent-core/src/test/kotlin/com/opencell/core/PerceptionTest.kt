package com.opencell.core

import com.opencell.core.model.Bounds
import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.perception.ElementResolver
import com.opencell.core.perception.PointResult
import com.opencell.core.perception.Resolution
import com.opencell.core.perception.ScreenGeometryMapper
import com.opencell.core.perception.ScreenInsets
import com.opencell.core.perception.ScreenState
import com.opencell.core.perception.Verifier
import com.opencell.core.protocol.Expectation
import com.opencell.core.protocol.Role
import com.opencell.core.protocol.Selector
import com.opencell.core.security.Redactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerceptionTest {
    private fun n(id: String, text: String?, top: Int, clickable: Boolean = false, editable: Boolean = false, visible: Boolean = true) =
        UiNode(id, text, null, null, null, Bounds(0, top, 100, top + 50), clickable = clickable, editable = editable, visible = visible)

    private fun snap(vararg nodes: UiNode, ts: Long = 1000, w: Int = 1080, h: Int = 2400) =
        UiSnapshot(1, ts, "pkg", w, h, nodes.toList())

    @Test fun resolvesUniqueNode() {
        val r = ElementResolver.resolve(Selector(text = "enviar"), snap(n("a", "Enviar", 0, true), n("b", "Cancelar", 100, true)))
        assertEquals("a", (r as Resolution.Found).node.id)
    }

    @Test fun duplicateTextIsAmbiguousAndIndexDisambiguates() {
        val s = snap(n("a", "Abrir", 0, true), n("b", "Abrir", 100, true))
        assertTrue(ElementResolver.resolve(Selector(text = "Abrir"), s) is Resolution.Ambiguous)
        assertEquals("b", (ElementResolver.resolve(Selector(text = "Abrir", index = 1), s) as Resolution.Found).node.id)
    }

    @Test fun nestedNodesWithSameBoundsAreNotAmbiguous() {
        val s = snap(n("parent", null, 0, clickable = true).copy(description = "Enviar"), n("child", "Enviar", 0))
        assertTrue(ElementResolver.resolve(Selector(text = "Enviar"), s) is Resolution.Found)
    }

    @Test fun invisibleAndEmptySelectorsNotFound() {
        assertEquals(Resolution.NotFound, ElementResolver.resolve(Selector(text = "X"), snap(n("a", "X", 0, visible = false))))
        assertEquals(Resolution.NotFound, ElementResolver.resolve(Selector(), snap(n("a", "X", 0))))
    }

    @Test fun roleFilter() {
        val s = snap(n("lbl", "Nome", 0), n("in", "", 100, editable = true))
        assertEquals("in", (ElementResolver.resolve(Selector(role = Role.INPUT), s) as Resolution.Found).node.id)
    }

    @Test fun geometryMapsNormalizedPoint() {
        val s = snap(ts = 1000)
        val r = ScreenGeometryMapper.toPixels(0.5f, 0.5f, s, ScreenState(1080, 2400, ScreenInsets(80, 120), 2000))
        assertEquals(PointResult.Ok(540, 1200), r)
    }

    @Test fun geometryRejectsUnsafePoints() {
        val s = snap(ts = 1000)
        val cur = ScreenState(1080, 2400, ScreenInsets(80, 120), 2000)
        assertTrue(ScreenGeometryMapper.toPixels(1.2f, 0.5f, s, cur) is PointResult.Rejected)
        assertTrue(ScreenGeometryMapper.toPixels(0.5f, 0.01f, s, cur) is PointResult.Rejected)
        assertTrue(ScreenGeometryMapper.toPixels(0.5f, 0.99f, s, cur) is PointResult.Rejected)
        assertTrue(ScreenGeometryMapper.toPixels(0.5f, 0.5f, s, cur.copy(width = 2400, height = 1080)) is PointResult.Rejected)
        assertTrue(ScreenGeometryMapper.toPixels(0.5f, 0.5f, s, cur.copy(nowMs = 60_000)) is PointResult.Rejected)
    }

    @Test fun verifierChecksTextAndFields() {
        val before = snap(n("a", "Antes", 0))
        val after = snap(n("a", "Enviado", 0), n("in", "Olá mundo", 100, editable = true))
        assertTrue(Verifier.check(Expectation.TextVisible("enviado"), before, after).ok)
        assertFalse(Verifier.check(Expectation.TextAbsent("enviado"), before, after).ok)
        assertTrue(Verifier.check(Expectation.FieldContains(Selector(role = Role.INPUT), "olá"), before, after).ok)
        assertTrue(Verifier.check(Expectation.ScreenChanged, before, after).ok)
        assertFalse(Verifier.check(Expectation.ScreenChanged, after, after).ok)
        assertFalse(Verifier.check(Expectation.PackageIs("outro"), before, after).ok)
    }

    @Test fun redactorMasksSecrets() {
        val out = Redactor.redact("key sk-abcdefghijklmnopqrstuvwxyz123456 AIzaSyA1234567890abcdefghijk Bearer abcdef1234567890xyz cpf 123.456.789-09 senha: hunter2")
        assertFalse(out.contains("abcdefghijklmnop"))
        assertFalse(out.contains("AIzaSy"))
        assertFalse(out.contains("123.456.789-09"))
        assertFalse(out.contains("hunter2"))
    }
}
