package com.opencell.core.engine

enum class AgentState {
    IDLE, LISTENING, UNDERSTANDING, PLANNING, WAITING_FOR_PERMISSION, WAITING_FOR_APPROVAL,
    OBSERVING, EXECUTING, VERIFYING, RECOVERING, PAUSED, COMPLETED, FAILED, CANCELLED;

    val isTerminal get() = this == COMPLETED || this == FAILED || this == CANCELLED
}

class IllegalTransitionException(from: AgentState, to: AgentState) :
    IllegalStateException("Transição inválida: $from -> $to")

class AgentStateMachine(initial: AgentState = AgentState.IDLE) {
    var state: AgentState = initial
        private set
    private var resumeTo: AgentState? = null

    fun canTransition(to: AgentState): Boolean = to in allowed(state)

    fun transition(to: AgentState): AgentState {
        if (state == to) return state
        if (!canTransition(to)) throw IllegalTransitionException(state, to)
        if (to == AgentState.PAUSED) resumeTo = state
        state = to
        return state
    }

    fun resume(): AgentState {
        val target = resumeTo ?: AgentState.OBSERVING
        resumeTo = null
        return transition(if (target == AgentState.PAUSED) AgentState.OBSERVING else target)
    }

    fun reset() {
        state = AgentState.IDLE
        resumeTo = null
    }

    companion object {
        private val live = setOf(
            AgentState.UNDERSTANDING, AgentState.PLANNING, AgentState.WAITING_FOR_PERMISSION, AgentState.WAITING_FOR_APPROVAL,
            AgentState.OBSERVING, AgentState.EXECUTING, AgentState.VERIFYING, AgentState.RECOVERING,
        )
        private val terminals = setOf(AgentState.FAILED, AgentState.CANCELLED)

        fun allowed(from: AgentState): Set<AgentState> = when (from) {
            AgentState.IDLE -> setOf(AgentState.LISTENING, AgentState.UNDERSTANDING, AgentState.CANCELLED)
            AgentState.LISTENING -> setOf(AgentState.UNDERSTANDING, AgentState.IDLE, AgentState.CANCELLED)
            AgentState.UNDERSTANDING -> setOf(AgentState.PLANNING, AgentState.FAILED, AgentState.CANCELLED, AgentState.PAUSED)
            AgentState.PLANNING -> setOf(
                AgentState.WAITING_FOR_PERMISSION, AgentState.WAITING_FOR_APPROVAL, AgentState.OBSERVING,
                AgentState.COMPLETED, AgentState.PAUSED,
            ) + terminals
            AgentState.WAITING_FOR_PERMISSION -> setOf(AgentState.OBSERVING, AgentState.PLANNING, AgentState.PAUSED) + terminals
            AgentState.WAITING_FOR_APPROVAL -> setOf(
                AgentState.OBSERVING, AgentState.EXECUTING, AgentState.PLANNING, AgentState.PAUSED,
            ) + terminals
            AgentState.OBSERVING -> setOf(
                AgentState.PLANNING, AgentState.WAITING_FOR_APPROVAL, AgentState.WAITING_FOR_PERMISSION, AgentState.EXECUTING,
                AgentState.RECOVERING, AgentState.COMPLETED, AgentState.PAUSED,
            ) + terminals
            AgentState.EXECUTING -> setOf(AgentState.VERIFYING, AgentState.OBSERVING, AgentState.RECOVERING, AgentState.PAUSED) + terminals
            AgentState.VERIFYING -> setOf(
                AgentState.OBSERVING, AgentState.RECOVERING, AgentState.COMPLETED, AgentState.PAUSED,
            ) + terminals
            AgentState.RECOVERING -> setOf(AgentState.OBSERVING, AgentState.PLANNING, AgentState.PAUSED) + terminals
            AgentState.PAUSED -> live + terminals
            AgentState.COMPLETED, AgentState.FAILED, AgentState.CANCELLED -> setOf(AgentState.IDLE)
        }
    }
}
