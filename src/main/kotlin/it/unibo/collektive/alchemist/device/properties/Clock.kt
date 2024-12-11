package it.unibo.collektive.alchemist.device.properties

interface Clock {
    fun currentClock(): ExecutionClock

    fun nextClock(): ExecutionClock
}

data class ExecutionClock(
    val time: Int = 0,
    val direction: Cycle = Cycle.BACKWARD
) {
    fun next(): ExecutionClock = when (direction) {
        Cycle.FORWARD -> ExecutionClock(time + 1, direction)
        Cycle.BACKWARD -> ExecutionClock(time - 1, direction)
    }
}

enum class Cycle {
    FORWARD,
    BACKWARD
}