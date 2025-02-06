package it.unibo.collektive.utils

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.DeviceSpawn
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.ResourceSensor
import it.unibo.common.calculateAngle
import it.unibo.common.minus
import it.unibo.common.plus
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

typealias SpawnerNoStability = Aggregate<Int>.(devSpawn: DeviceSpawn, locationSensor: LocationSensor, potential: Double, localSuccess: Double, success: Double, localResource: Double) -> Unit

// data class Stability(val spawnStable: Boolean = false, val destroyStable: Boolean = false) {
//    infix fun and(other: Stability): Boolean = spawnStable && other.spawnStable && destroyStable && other.destroyStable
// }

/**
 * The policies that determine if a node should be spawned or destroyed.
 * The node is spawned if the local resources are above the lower bound threshold,
 * if it has less than a maximum threshold of children and the neighborhood is stable.
 * The node is destroyed if the local resources are below the lower bound,
 * if it is not father of any node and the neighborhood is stable.
 */
fun Aggregate<Int>.determineSpawn(
    childrenCount: Int,
    localResource: Double,
    localPosition: Pair<Double, Double>,
    neighborPositions: List<Pair<Double, Double>>,
    devSpawn: DeviceSpawn,
    random: RandomGenerator,
    resourceS: ResourceSensor,
) {
    if (neighborPositions.isEmpty() ||
        localResource / (2 + childrenCount) > resourceS.resourceLowerBound &&
        childrenCount < devSpawn.maxChildren
    ) {
        val relativePositions = neighborPositions.map { it - localPosition }
        val angles = relativePositions.map { atan2(it.second, it.first) }.sorted()
        val angle = calculateAngle(angles, random, devSpawn.maxChildren)
        if (!angle.isNaN()) {
            val x = devSpawn.cloningRange * cos(angle)
            val y = devSpawn.cloningRange * sin(angle)
            val absoluteDestination = localPosition + (x to y)
            devSpawn.spawn(absoluteDestination)
        }
    }
}
