package it.unibo.collektive.alchemist.device.properties.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.alchemist.device.properties.Clock
import it.unibo.collektive.alchemist.device.properties.Cycle.SPAWNED
import it.unibo.collektive.alchemist.device.properties.ExecutionClock

class ExecutionClockProperty<T, P : Position<P>>(
    override val node: Node<T>,
    private val environment: Environment<T, P>,
) : ExecutionClock, NodeProperty<T> {
    private var clock: Clock = Clock().also { node.setConcentration(SimpleMolecule("clock"), it as T) }

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        ExecutionClockProperty(node, environment)

    override fun currentClock(): Clock {
        return clock.also { node.setConcentration(SimpleMolecule("clock"), it as T) }
    }

    override fun nextClock() {
        clock = clock.next()
            .also { node.setConcentration(SimpleMolecule("clock"), it as T) }
    }

    override fun justSpawned(time: Int) {
        clock = Clock(time = time, action = SPAWNED)
            .also { node.setConcentration(SimpleMolecule("clock"), it as T) }
    }

    override fun toString(): String = clock.toString()
}
