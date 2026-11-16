package com.opencell.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Selector(
    val text: String? = null,
    val textContains: String? = null,
    val description: String? = null,
    val viewId: String? = null,
    val role: Role? = null,
    val index: Int? = null,
) {
    val isEmpty get() = text == null && textContains == null && description == null && viewId == null && role == null
}

@Serializable
enum class Role { BUTTON, INPUT, LIST, ANY }

@Serializable
enum class Direction { UP, DOWN, LEFT, RIGHT }

@Serializable
sealed interface AgentAction {
    @Serializable @SerialName("open_application")
    data class OpenApplication(val appName: String? = null, val packageName: String? = null) : AgentAction

    @Serializable @SerialName("tap_element")
    data class TapElement(val selector: Selector) : AgentAction

    @Serializable @SerialName("tap_point")
    data class TapPoint(val x: Float, val y: Float, val snapshotId: Long) : AgentAction

    @Serializable @SerialName("long_press")
    data class LongPress(val selector: Selector) : AgentAction

    @Serializable @SerialName("swipe")
    data class Swipe(val direction: Direction) : AgentAction

    @Serializable @SerialName("scroll_container")
    data class ScrollContainer(val direction: Direction, val selector: Selector? = null) : AgentAction

    @Serializable @SerialName("enter_text")
    data class EnterText(val selector: Selector, val text: String) : AgentAction

    @Serializable @SerialName("press_back")
    data object PressBack : AgentAction

    @Serializable @SerialName("wait_for_ui_change")
    data class WaitForUiChange(val timeoutMs: Long = 3000) : AgentAction

    @Serializable @SerialName("request_user_confirmation")
    data class RequestUserConfirmation(val message: String) : AgentAction

    @Serializable @SerialName("report_limitation")
    data class ReportLimitation(val message: String) : AgentAction

    @Serializable @SerialName("complete_task")
    data class CompleteTask(val summary: String) : AgentAction
}

@Serializable
sealed interface Expectation {
    @Serializable @SerialName("text_visible")
    data class TextVisible(val text: String) : Expectation

    @Serializable @SerialName("text_absent")
    data class TextAbsent(val text: String) : Expectation

    @Serializable @SerialName("element_exists")
    data class ElementExists(val selector: Selector) : Expectation

    @Serializable @SerialName("field_contains")
    data class FieldContains(val selector: Selector, val text: String) : Expectation

    @Serializable @SerialName("package_is")
    data class PackageIs(val packageName: String) : Expectation

    @Serializable @SerialName("screen_changed")
    data object ScreenChanged : Expectation

    @Serializable @SerialName("scrolled")
    data object Scrolled : Expectation
}

@Serializable
data class PlanStep(
    val id: String,
    val description: String,
    val action: AgentAction,
    val expect: Expectation? = null,
)

@Serializable
data class Plan(val version: Int = 1, val goal: String, val steps: List<PlanStep>)
