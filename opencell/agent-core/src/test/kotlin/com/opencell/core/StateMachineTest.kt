package com.opencell.core

import com.opencell.core.engine.AgentState
import com.opencell.core.engine.AgentStateMachine
import com.opencell.core.engine.IllegalTransitionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StateMachineTest {
    @Test fun happyPath() {
        val m = AgentStateMachine()
        listOf(
            AgentState.UNDERSTANDING, AgentState.PLANNING, AgentState.OBSERVING, AgentState.EXECUTING,
            AgentState.VERIFYING, AgentState.OBSERVING, AgentState.COMPLETED,
        ).forEach { m.transition(it) }
        assertEquals(AgentState.COMPLETED, m.state)
    }

    @Test(expected = IllegalTransitionException::class) fun cannotExecuteFromIdle() {
        AgentStateMachine().transition(AgentState.EXECUTING)
    }

    @Test fun terminalStatesOnlyGoToIdle() {
        listOf(AgentState.COMPLETED, AgentState.FAILED, AgentState.CANCELLED).forEach { t ->
            assertEquals(setOf(AgentState.IDLE), AgentStateMachine.allowed(t))
        }
    }

    @Test fun pauseResumeReturnsToPreviousState() {
        val m = AgentStateMachine()
        m.transition(AgentState.UNDERSTANDING); m.transition(AgentState.PLANNING); m.transition(AgentState.OBSERVING)
        m.transition(AgentState.EXECUTING)
        m.transition(AgentState.PAUSED)
        assertEquals(AgentState.EXECUTING, m.resume())
    }

    @Test fun cancelReachableFromEveryLiveState() {
        val live = AgentState.values().filter { !it.isTerminal && it != AgentState.IDLE && it != AgentState.LISTENING && it != AgentState.UNDERSTANDING }
        live.forEach { assertTrue("$it", AgentStateMachine(it).canTransition(AgentState.CANCELLED)) }
        assertFalse(AgentStateMachine(AgentState.COMPLETED).canTransition(AgentState.EXECUTING))
    }
}
