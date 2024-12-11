@file:Suppress("UNCHECKED_CAST")

package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.times.DoubleTime
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.utils.Stability
import it.unibo.common.calculateAngle
import it.unibo.common.minus
import it.unibo.common.plus
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.nextUp
import kotlin.math.sin

class Spawn<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    val randomGenerator: RandomGenerator,
    val locationSensor: LocationSensor,
    val cloningRange: Double,
    val maxChildren: Int,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>
    ): Action<T> =
        Spawn(environment, node, randomGenerator, locationSensor, cloningRange, maxChildren)

    override fun execute() {
        // todo check if its the node's turn
        // todo check if this node is the one with maximum resources
        val localPosition: Pair<Double, Double> = locationSensor.coordinates()
        val neighborPosition: List<Pair<Double, Double>> = locationSensor.surroundings()
        val relativePositions: List<Pair<Double, Double>> = neighborPosition.map { it - localPosition}
        val angles = relativePositions.map { atan2(it.second, it.first) }.sorted()
        val angle = calculateAngle(angles, randomGenerator, maxChildren)
        when {
            angle.isNaN() -> Stability(spawnStable = true, destroyStable = true)
            else -> {
                val x = cloningRange * cos(angle)
                val y = cloningRange * sin(angle)
                val absoluteDestination = localPosition + (x to y)
                spawn(absoluteDestination)
            }
        }
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD

    private fun spawn(
        coordinate: Pair<Double, Double>,
    ): Double {
        val spawningTime = environment.simulation.time + DoubleTime(randomGenerator.nextRandomDouble(0.0.nextUp() .. 0.1))
        val cloneOfThis = node.cloneNode(spawningTime)
        cloneOfThis.setConcentration(SimpleMolecule("leader"), false as T)
        cloneOfThis.setConcentration(SimpleMolecule("parent"), node.id as T)
        cloneOfThis.setConcentration(SimpleMolecule("weight"), 1.0 as T)
        node.setConcentration(SimpleMolecule("leaf"), false as T)
        node.setConcentration(SimpleMolecule("intermediate"), true as T)
        val updatedPosition = environment.makePosition(*coordinate.toList().toTypedArray())
        environment.addNode(cloneOfThis, updatedPosition)
        return spawningTime.toDouble()
    }
}