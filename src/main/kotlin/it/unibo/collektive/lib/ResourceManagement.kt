package it.unibo.collektive.lib

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LeaderSensor
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.alchemist.device.sensors.ResourceSensor
import it.unibo.collektive.coordination.spreadToChildren

/**
 * Spreads the available resources to the children of this device, according to the [localSuccess] of each child.
 */
context(ResourceSensor, LeaderSensor, LocationSensor, EnvironmentVariables)
fun <ID : Comparable<ID>> Aggregate<ID>.spreadResource(
    potential: Double,
    localSuccess: Double,
): Double {
    return spreadToChildren(potential, if (potential > 0) 0.0 else getResource(), localSuccess).also {
        setCurrentOverallResource(it)
    }
}
