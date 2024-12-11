package it.unibo.collektive.alchemist.device.properties.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.collektive.alchemist.device.properties.Clock
import it.unibo.collektive.alchemist.device.properties.ExecutionClock

class ClockProperty<T, P : Position<P>>(
    override val node: Node<T>,
    private val environment: Environment<T, P>,
) : Clock, NodeProperty<T> {
    private var clock: ExecutionClock = ExecutionClock()

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        ClockProperty(node, environment)

    override fun currentClock(): ExecutionClock = clock

    override fun nextClock(): ExecutionClock {
        clock = clock.next()
        return clock
    }
}
