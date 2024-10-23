package it.unibo.collektive.alchemist.device.sensors.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.times.DoubleTime
import it.unibo.alchemist.util.RandomGenerators.nextDouble
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.DeviceSpawn
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.nextUp

class DeviceSpawner<T, P : Position<P>> @JvmOverloads constructor(
    private val randomGenerator: RandomGenerator,
    private val environment: Environment<T, P>,
    override val node: Node<T>,
    override val cloningRange: Double = 1.0,
    override val maxChildren: Int,
    override val minSpawnWait: Double = 20.0,
) : DeviceSpawn, NodeProperty<T> {
    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        DeviceSpawner(randomGenerator, environment, node, cloningRange, maxChildren, minSpawnWait)

    override fun spawn(
        coordinate: Pair<Double, Double>,
    ): Double {
        val spawningTime = environment.simulation.time + DoubleTime(randomGenerator.nextDouble(0.0.nextUp(), 0.1))
        val cloneOfThis = node.cloneNode(spawningTime)
        val updatedPosition = environment.makePosition(*coordinate.toList().toTypedArray())
        environment.addNode(cloneOfThis, updatedPosition)
        return spawningTime.toDouble()
    }

    override fun <ID : Comparable<ID>> Aggregate<ID>.selfDestroy() {
        node.reactions.toList().forEach {
            environment.simulation.reactionRemoved(it)
            node.removeReaction(it)
        }
        environment.simulation.schedule { environment.removeNode(node) }
    }

    override fun currentTime(): Double = environment.simulation.time.toDouble()
}
