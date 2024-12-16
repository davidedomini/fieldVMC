package it.unibo.collektive.alchemist.device.properties

import it.unibo.collektive.alchemist.device.properties.Cycle.BACKWARD
import it.unibo.collektive.alchemist.device.properties.Cycle.FORWARD
import it.unibo.collektive.alchemist.device.properties.Cycle.SPAWNED

/**
 * Evaluates the current and next [Clock] of a node.
 */
interface ExecutionClock {
    /**
     * Returns the current [Clock].
     */
    fun currentClock(): Clock

    /**
     * Set the next [Clock].
     */
    fun nextClock()
}

/**
 * A clock that keeps track of the current [time] and [action] of a node.
 */
data class Clock(
    val time: Int = 0,
    val action: Cycle = SPAWNED,
) {
    /**
     * Evaluates and returns the next [Clock].
     */
    fun next(): Clock =
        when (action) {
            BACKWARD -> Clock(time, action.reverse())
            FORWARD -> Clock(time + 1, action.reverse())
            SPAWNED -> Clock(time + 1, action.reverse())
        }
}

/**
 * The direction of the action of a node.
 **/
enum class Cycle {
    /**
     * The node is moving success towards the parent.
     */
    BACKWARD,

    /**
     * The node is moving resources towards the children.
     */
    FORWARD,

    /**
     * The node has just been spawned.
     */
    SPAWNED,

    ;

    /**
     * Returns the opposite direction of the current one.
     */
    fun reverse(): Cycle =
        when (this) {
            FORWARD -> BACKWARD
            BACKWARD -> FORWARD
            SPAWNED -> BACKWARD
        }
}
