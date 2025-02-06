package it.unibo.collektive.alchemist.device.properties

import it.unibo.collektive.alchemist.device.properties.Cycle.BACKWARD
import it.unibo.collektive.alchemist.device.properties.Cycle.FORWARD
import it.unibo.collektive.alchemist.device.properties.Cycle.MAX
import it.unibo.collektive.alchemist.device.properties.Cycle.SPAWNING

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

    fun justSpawned(time: Int)
}

/**
 * A clock that keeps track of the current [time] and [action] of a node.
 */
data class Clock(
    val time: Int = 0,
    val action: Cycle = SPAWNING,
) {
    /**
     * Evaluates and returns the next [Clock].
     */
    fun next(): Clock =
        when (action) {
            BACKWARD -> Clock(time, action.nextStep())
            FORWARD -> Clock(time, action.nextStep())
            MAX -> Clock(time, action.nextStep())
            SPAWNING -> Clock(time + 1, action.nextStep())
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
     * The phase to decide which node has the maximum resource.
     */
    MAX,

    /**
     * The node has just been spawned.
     */
    SPAWNING,

    ;

    /**
     * Returns the opposite direction of the current one.
     */
    fun nextStep(): Cycle =
        when (this) {
            BACKWARD -> FORWARD
            FORWARD -> MAX
            MAX -> SPAWNING
            SPAWNING -> BACKWARD
        }
}
