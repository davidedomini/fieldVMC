package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Node.Companion.asProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.times.DoubleTime
import it.unibo.collektive.alchemist.device.properties.Cycle.MAX
import it.unibo.collektive.alchemist.device.properties.Cycle.SPAWNING
import it.unibo.collektive.alchemist.device.properties.impl.ExecutionClockProperty
import it.unibo.collektive.alchemist.device.sensors.impl.LocationSensorProperty
import it.unibo.collektive.alchemist.device.sensors.impl.RandomNodeProperty
import it.unibo.collektive.utils.Stability
import it.unibo.common.calculateAngle
import it.unibo.common.minus
import it.unibo.common.plus
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.nextUp
import kotlin.math.sin

class Spawn<T : Any, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    private val randomGenerator: RandomNodeProperty<T>,
    private val locationSensor: LocationSensorProperty<T, P>,
    private val cloningRange: Double,
    private val resourceThreshold: Double,
    private val maxChildren: Int,
    private val minSpawnWait: Int,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> =
        Spawn(
            environment,
            node,
            randomGenerator,
            node.asProperty<T, LocationSensorProperty<T, P>>(),
            cloningRange,
            resourceThreshold,
            maxChildren,
            minSpawnWait,
        )

    override fun execute() {
        val neighbors = environment.getNeighborhood(node)
            .filterNot { n -> n.id == node.id }
            .map { it to it.asProperty<T, ExecutionClockProperty<T, P>>() }
        val current = node.asProperty<T, ExecutionClockProperty<T, P>>().currentClock()
        val parent = neighbors.firstOrNull { (n, _) -> node.getConcentration(SimpleMolecule("parent")) == n.id } // if null then it is root
        val children = neighbors.filter { (n, _) -> n.getConcentration(SimpleMolecule("parent")) == node.id }
        if ((children.isNotEmpty() && current.action == MAX && parent != null && parent.second.currentClock().action == SPAWNING)) { // intermediate node going towards the leaves
            node.setConcentration(SimpleMolecule("max-leaf-id"), parent!!.first.getConcentration(SimpleMolecule("max-leaf-id")))
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        } else if (children.isNotEmpty() && current.action == MAX && parent == null) { // root with children
            // max id already set in find max
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        } else if (children.isEmpty() && current.action == MAX && parent == null) { // root as leaf
            // should spawn
            val localResource = node.getConcentration(SimpleMolecule("resource")) as Double
            spawnChildren(localResource)
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        } else if (parent != null && parent.second.currentClock().action == SPAWNING && children.isEmpty() && current.action == MAX) { // I am a leaf
            if (parent.first.getConcentration(SimpleMolecule("max-leaf-id")) == node.id && current.time % minSpawnWait == 0) { // I'm the max
                // I should spawn
                val localResource = node.getConcentration(SimpleMolecule("resource")) as Double
                spawnChildren(localResource)
            }
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        }
    }

    private fun spawnChildren(localResource: Double) {
        val spawnableChildren = (localResource / resourceThreshold).toInt().coerceIn(1, maxChildren)
        val localPosition: Pair<Double, Double> = locationSensor.coordinates()
        val neighborPosition: List<Pair<Double, Double>> = locationSensor.surroundings()
        val relativePositions: List<Pair<Double, Double>> = neighborPosition.map { it - localPosition }
        val angles = relativePositions.map { atan2(it.second, it.first) }.sorted()
        repeat(spawnableChildren) {
            val angle = calculateAngle(angles, randomGenerator, maxChildren)
            when {
                !angle.isNaN() -> {
                    val x = cloningRange * cos(angle)
                    val y = cloningRange * sin(angle)
                    val absoluteDestination = localPosition + (x to y)
                    spawn(absoluteDestination)
                }
                else -> { }
            }

        }
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD

    private fun spawn(coordinate: Pair<Double, Double>) {
        val spawningTime = environment.simulation.time + DoubleTime(randomGenerator.nextRandomDouble(0.0.nextUp() .. 0.1))
        val cloneOfThis = node.cloneNode(spawningTime)
        val nodeClock = node.asProperty<T, ExecutionClockProperty<T, P>>().currentClock()
        cloneOfThis.setConcentration(SimpleMolecule("parent"), node.id as T)
        cloneOfThis.setConcentration(SimpleMolecule("weight"), 0.01 as T)
        cloneOfThis.setConcentration(SimpleMolecule("resource"), 0.0 as T)
        cloneOfThis.asProperty<T, ExecutionClockProperty<T, P>>().justSpawned(nodeClock.time)
        val updatedPosition = environment.makePosition(*coordinate.toList().toTypedArray())
        environment.addNode(cloneOfThis, updatedPosition)
    }
}
