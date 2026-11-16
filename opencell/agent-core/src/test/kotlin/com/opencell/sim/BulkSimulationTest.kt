package com.opencell.sim

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class BulkSimulationTest {
    @Test fun referencePlannerBulk() {
        val n = System.getenv("OC_SIM_N")?.toIntOrNull(); assumeTrue(n != null)
        val seed = System.getenv("OC_SIM_SEED")?.toIntOrNull() ?: 42
        val noise = System.getenv("OC_SIM_NOISE")?.toIntOrNull() ?: 0
        val tasks = SimTasks.generate(n!!, seed = seed)
        val out = File(System.getenv("OC_SIM_OUT") ?: "/tmp/sim-ref.log")
        val results = SimRunner.runAll(tasks, seed = seed, faults = true, concurrency = 8, planner = { t, p -> ReferencePlanner(t, p, noise, kotlin.random.Random(seed + t.id)) }, out = out)
        val rep = SimRunner.report(results)
        File(out.path + ".summary").writeText(rep); println(rep)
    }
}
