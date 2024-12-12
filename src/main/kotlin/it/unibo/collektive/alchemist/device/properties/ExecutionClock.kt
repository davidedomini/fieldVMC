package it.unibo.collektive.alchemist.device.properties

import it.unibo.collektive.alchemist.device.properties.Cycle.BACKWARD
import it.unibo.collektive.alchemist.device.properties.Cycle.FORWARD

interface ExecutionClock {
    fun currentClock(): Clock

    fun nextClock(): Clock
}

data class Clock(
    val time: Int = 0,
    val direction: Cycle = BACKWARD
) {
    fun next(): Clock = when (direction) {
        FORWARD -> Clock(time, direction.reverse())
        BACKWARD -> Clock(time + 1, direction.reverse())
    }
}

enum class Cycle {
    FORWARD,
    BACKWARD;

    fun reverse(): Cycle = when (this) {
        FORWARD -> BACKWARD
        BACKWARD -> FORWARD
    }
}