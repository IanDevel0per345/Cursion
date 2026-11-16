package com.opencell.sim

import com.opencell.core.engine.ApprovalGate
import com.opencell.core.engine.Orchestrator
import com.opencell.core.engine.OrchestratorConfig
import com.opencell.core.engine.Outcome
import com.opencell.core.engine.Planner
import com.opencell.core.engine.TaskEvent
import com.opencell.core.engine.TaskJournal
import com.opencell.core.policy.AutonomyMode
import com.opencell.core.policy.PolicyEngine
import com.opencell.core.policy.PolicySettings
import com.opencell.core.policy.Risk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.random.Random

data class SimResult(val task: SimTask, val ok: Boolean, val outcome: String, val summary: String, val log: String, val approvals: Int, val ms: Long, val error: String? = null)

object SimRunner {
    class CountingGate : ApprovalGate {
        @Volatile var asked = 0
        override suspend fun request(taskId: String, stepId: String, reason: String, risk: Risk): Boolean { asked++; return true }
    }

    suspend fun runOne(task: SimTask, seed: Int, faults: Boolean, planner: (SimTask, SimPhone) -> Planner, timeoutMs: Long = 600_000): SimResult {
        val rnd = Random(seed * 7919 + task.id)
        val phone = SimPhone(rnd, faults)
        task.prepare(phone)
        val gate = CountingGate()
        val mode = if (task.id % 2 == 0) AutonomyMode.PERMISSIVE else AutonomyMode.SUPERVISED
        val events = mutableListOf<String>()
        val orch = Orchestrator(phone, phone, planner(task, phone), gate, TaskJournal { e -> if (e is TaskEvent.StepFinished) events += "${e.result.status}:${e.result.description}:${e.result.detail}".take(120) },
            PolicyEngine(emptySet()), { OrchestratorConfig(maxSteps = 60, settleDelayMs = 0, policy = PolicySettings(mode = mode)) })
        val t0 = System.currentTimeMillis()
        return try {
            val report = withTimeoutOrNull(timeoutMs) { orch.run(task.goal) }
                ?: return SimResult(task, false, "TIMEOUT", "", phone.log.joinToString(), gate.asked, System.currentTimeMillis() - t0)
            val goalOk = task.check(phone)
            val outcomeOk = if (task.expectPartial) report.outcome == Outcome.PARTIAL else report.outcome == Outcome.SUCCESS
            SimResult(task, goalOk && outcomeOk, report.outcome.name + (if (goalOk) "" else "/estado-errado"), report.summary,
                phone.log.joinToString(" > ") + " || " + events.takeLast(4).joinToString(" | "), gate.asked, System.currentTimeMillis() - t0)
        } catch (e: Throwable) {
            SimResult(task, false, "EXCEPTION", "", phone.log.joinToString(), gate.asked, System.currentTimeMillis() - t0, e.toString().take(200))
        }
    }

    fun runAll(tasks: List<SimTask>, seed: Int, faults: Boolean, concurrency: Int, planner: (SimTask, SimPhone) -> Planner, out: File): List<SimResult> = runBlocking(Dispatchers.Default) {
        val sem = Semaphore(concurrency)
        out.writeText("")
        tasks.map { t -> async { sem.withPermit { runOne(t, seed, faults, planner).also { r ->
            synchronized(out) { out.appendText("${if (r.ok) "OK  " else "FAIL"} #${t.id} ${t.kind} [${r.outcome}] ${t.goal}\n   ${r.summary.take(160)}\n   ${r.log.take(500)}${r.error?.let { "\n   ERR $it" } ?: ""}\n") }
        } } } }.awaitAll()
    }

    fun report(results: List<SimResult>): String = buildString {
        val total = results.size; val ok = results.count { it.ok }
        appendLine("TOTAL $total  OK $ok  FALHAS ${total - ok}  (${"%.1f".format(100.0 * ok / total)}%)")
        results.groupBy { it.task.kind }.toSortedMap().forEach { (k, v) -> appendLine("  %-14s %4d/%-4d %s".format(k, v.count { it.ok }, v.size, v.filter { !it.ok }.groupBy { it.outcome }.mapValues { it.value.size })) }
        appendLine("aprovações pedidas: ${results.sumOf { it.approvals }}  tempo médio: ${results.map { it.ms }.average().toLong()}ms")
    }
}
