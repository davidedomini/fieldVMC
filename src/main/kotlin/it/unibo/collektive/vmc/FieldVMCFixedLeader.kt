package it.unibo.collektive.vmc

import it.unibo.alchemist.collektive.device.DistanceSensor
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.*
import it.unibo.collektive.coordination.findParent
import it.unibo.collektive.field.Field.Companion.fold
import it.unibo.collektive.lib.convergeSuccess
import it.unibo.collektive.lib.findPotential
import it.unibo.collektive.lib.obtainLocalSuccess
import it.unibo.collektive.lib.spreadResource

/**
 * Entrypoint of the VMC algorithm, using spawning and destroying after stability policies.
 */
fun Aggregate<Int>.fixedRootWithSpawning(
    devSpawn: DeviceSpawn,
    distanceS: DistanceSensor,
    env: EnvironmentVariables,
    locationS: LocationSensor,
    random: RandomGenerator,
    resourceS: ResourceSensor,
    successS: SuccessSensor,
): Double = fixedRootStability(devSpawn, distanceS, env, locationS, random, resourceS, successS)

fun Aggregate<Int>.fixedRootStability(
    devSpawn: DeviceSpawn,
    distanceS: DistanceSensor,
    env: EnvironmentVariables,
    locationS: LocationSensor,
    random: RandomGenerator,
    resourceS: ResourceSensor,
    successS: SuccessSensor,
): Double = with(distanceS) {
    vmcFixedLeader(devSpawn, distanceS, env, locationS, resourceS, successS) { devSpawn, locationS, potential: Double, localSuccess: Double, success: Double, localResource: Double ->
        val children = neighboring(findParent(potential))
        env["children-around"] = children
        env["myParent"] = children.localValue
        val childrenCount = children
            .fold(0) { acc, parent -> acc + if (parent == localId) 1 else 0 }
        env["children-count"] = childrenCount
        val neighbors = neighboring(locationS.coordinates())
        val localPosition = neighbors.localValue
        val neighborPositions = locationS.surroundings()
        val now = devSpawn.currentTime()
        share(Stability()) { neighborhoodStability ->
            val lastChanged = evolve(now to listOf(potential, localSuccess, success, localResource)) { last ->
                val current = listOf(potential, localSuccess, success, localResource)
                if (current == last.second) { last } else { now to current }
            }.first
            val localStability = neighborhoodStability.localValue
            determineStability(childrenCount, localResource, lastChanged, now, potential, localPosition, neighborPositions, localStability, devSpawn, env, random, resourceS)
        }
    }
}

fun Aggregate<Int>.vmcFixedLeader(
    devSpawn: DeviceSpawn,
    distanceS: DistanceSensor,
    env: EnvironmentVariables,
    locationS: LocationSensor,
    resourceS: ResourceSensor,
    successS: SuccessSensor,
    spawner: Spawner,
): Double {
    val isLeader = env.get<Boolean>("leader")
    val potential = findPotential(distanceS, isLeader)
    val localSuccess = obtainLocalSuccess(successS)
    val success = convergeSuccess(successS, potential, localSuccess)
    val localResource = spreadResource(env, potential, success, resourceS.maxResource).also { env["local-resource"] = it }
    spawner(devSpawn, locationS, potential, localSuccess, success, localResource)
    return localResource
}