package it.unibo.collektive.vmc

import it.unibo.alchemist.collektive.device.DistanceSensor
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.DeviceSpawn
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.LeaderSensor
import it.unibo.collektive.alchemist.device.sensors.LocationSensor
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.ResourceSensor
import it.unibo.collektive.alchemist.device.sensors.SuccessSensor
import it.unibo.collektive.coordination.findParent
import it.unibo.collektive.field.Field.Companion.fold
import it.unibo.collektive.lib.convergeSuccess
import it.unibo.collektive.lib.findPotential
import it.unibo.collektive.lib.isLeader
import it.unibo.collektive.lib.obtainLocalSuccess
import it.unibo.collektive.lib.spreadResource
import it.unibo.collektive.utils.Spawner
import it.unibo.collektive.utils.Stability
import it.unibo.collektive.utils.determineStability

/**
 * Entrypoint of the VMC algorithm, using spawning and destroying after stability policies.
 */
fun Aggregate<Int>.withSpawning(
    distanceS: DistanceSensor,
    devSpawn: DeviceSpawn,
    env: EnvironmentVariables,
    leaderS: LeaderSensor,
    locationS: LocationSensor,
    random: RandomGenerator,
    resourceS: ResourceSensor,
    successS: SuccessSensor,
): Double = spawnAndDestroyAfterStability(devSpawn, distanceS, env, leaderS, locationS, random, resourceS, successS)

/**
 * Spawns a new node or destroys an old one if the conditions are met.
 * The node is spawned if the local resources are above the lower bound threshold,
 * if it has less than a maximum threshold of children and the neighborhood is stable.
 * The node is destroyed if the local resources are below the lower bound,
 * if it is not father of any node and the neighborhood is stable.
 */
fun Aggregate<Int>.spawnAndDestroyAfterStability(
    devSpawn: DeviceSpawn,
    distanceS: DistanceSensor,
    env: EnvironmentVariables,
    leaderS: LeaderSensor,
    locationS: LocationSensor,
    random: RandomGenerator,
    resourceS: ResourceSensor,
    successS: SuccessSensor,
): Double =
    with(distanceS) {
        vmc(
            devSpawn,
            distanceS,
            env,
            leaderS,
            locationS,
            resourceS,
            successS,
        ) { devSpawn, locationSensor, potential: Double, localSuccess: Double, success: Double, localResource: Double ->
            val children = neighboring(findParent(potential))
            env["children-around"] = children
            env["parent"] = children.localValue
            val childrenCount =
                children
                    .fold(0) { acc, parent -> acc + if (parent == localId) 1 else 0 }
            env["children-count"] = childrenCount
            val neighbors = neighboring(locationSensor.coordinates())
            val localPosition = neighbors.localValue
            val neighborPositions = locationSensor.surroundings()
            val now = devSpawn.currentTime()
            share(Stability()) { neighborhoodStability ->
                val lastChanged =
                    evolve(now to listOf(potential, localSuccess, success, localResource)) { last ->
                        val current = listOf(potential, localSuccess, success, localResource)
                        if (current == last.second) {
                            last
                        } else {
                            now to current
                        }
                    }.first
                val localStability = neighborhoodStability.localValue
                determineStability(
                    childrenCount,
                    localResource,
                    lastChanged,
                    now,
                    potential,
                    localPosition,
                    neighborPositions,
                    localStability,
                    devSpawn,
                    env,
                    random,
                    resourceS,
                )
            }
        }
    }

/**
 * The VMC algorithm with the spawning and destroying of nodes.
 * First it elects the leader, then it calculates the potential,
 * the local success, and the overall success of the children.
 * Finally, it calculates the local resource and checks the spawn and destroy policies.
 */
fun Aggregate<Int>.vmc(
    devSpawn: DeviceSpawn,
    distanceSensor: DistanceSensor,
    env: EnvironmentVariables,
    leaderSensor: LeaderSensor,
    locationSensor: LocationSensor,
    resourceSensor: ResourceSensor,
    successSensor: SuccessSensor,
    spawner: Spawner,
): Double {
    val isLeader = isLeader(distanceSensor, leaderSensor, resourceSensor)
    val potential = findPotential(distanceSensor, isLeader)
    val localSuccess = obtainLocalSuccess(successSensor)
    val success = convergeSuccess(successSensor, potential, localSuccess)
    val localResource = spreadResource(env, resourceSensor, potential, success)
    spawner(devSpawn, locationSensor, potential, localSuccess, success, localResource)
    return localResource
}
