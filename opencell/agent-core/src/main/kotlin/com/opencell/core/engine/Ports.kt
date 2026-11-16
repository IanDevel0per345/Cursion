package com.opencell.core.engine

import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.policy.PolicySettings
import com.opencell.core.policy.Risk
import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Plan
import com.opencell.core.protocol.PlanStep

interface ScreenObserver {
    suspend fun snapshot(): UiSnapshot?
}

sealed interface ExecResult {
    data object Dispatched : ExecResult
    data class Failed(val reason: String) : ExecResult
}

interface ActionExecutor {
    suspend fun execute(action: AgentAction, target: UiNode?, snapshot: UiSnapshot): ExecResult
    fun cancelPending() {}
}

sealed interface PlanResult {
    data class Ok(val plan: Plan) : PlanResult
    data class Failed(val reason: String) : PlanResult
}

data class ReplanContext(
    val goal: String,
    val snapshot: UiSnapshot?,
    val failedStep: PlanStep,
    val reason: String,
    val completed: List<String>,
    val userHint: String?,
    val continuation: Boolean = false,
)

interface Planner {
    suspend fun plan(goal: String, snapshot: UiSnapshot?): PlanResult
    suspend fun replan(context: ReplanContext): PlanResult =
        PlanResult.Failed("Este planejador não suporta replanejamento.")
}

interface ApprovalGate {
    suspend fun request(taskId: String, stepId: String, reason: String, risk: Risk): Boolean
}

enum class StepStatus { VERIFIED, UNVERIFIED, FAILED, BLOCKED, SKIPPED }

data class StepResult(val stepId: String, val description: String, val status: StepStatus, val detail: String)

enum class Outcome { SUCCESS, PARTIAL, FAILED, CANCELLED, DECLINED, BLOCKED }

data class TaskReport(
    val taskId: String,
    val goal: String,
    val outcome: Outcome,
    val summary: String,
    val steps: List<StepResult>,
    val startedAtMs: Long,
    val finishedAtMs: Long,
)

sealed interface TaskEvent {
    val taskId: String
    data class StateChanged(override val taskId: String, val state: AgentState) : TaskEvent
    data class PlanReady(override val taskId: String, val plan: Plan) : TaskEvent
    data class PlanExtended(override val taskId: String, val steps: List<PlanStep>) : TaskEvent
    data class StepStarted(override val taskId: String, val step: PlanStep) : TaskEvent
    data class StepFinished(override val taskId: String, val result: StepResult) : TaskEvent
    data class ApprovalRequested(override val taskId: String, val stepId: String, val reason: String) : TaskEvent
    data class Finished(override val taskId: String, val report: TaskReport) : TaskEvent
}

fun interface TaskJournal {
    fun onEvent(event: TaskEvent)
}

data class OrchestratorConfig(
    val maxSteps: Int = 40,
    val maxRetriesPerAction: Int = 2,
    val maxReplans: Int = 4,
    val maxContinuations: Int = 25,
    val maxRepeatSameAction: Int = 3,
    val settleDelayMs: Long = 400,
    val policy: PolicySettings = PolicySettings(),
)

interface Clock {
    fun nowMs(): Long
}

object SystemClock : Clock {
    override fun nowMs() = System.currentTimeMillis()
}
