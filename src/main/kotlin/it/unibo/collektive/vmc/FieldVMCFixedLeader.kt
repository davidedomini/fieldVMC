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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Entrypoint of the VMC algorithm, using spawning and destroying after stability policies.
 */
fun Aggregate<Int>.fixedRootWithSpawning(
    devSpawn: DeviceSpawn,
    distanceS: DistanceSensor,
    env: EnvironmentVariables,
    leaderS: LeaderSensor,
    locationS: LocationSensor,
    random: RandomGenerator,
    resourceS: ResourceSensor,
    successS: SuccessSensor,
): Double = fixedRootStability(devSpawn, distanceS, env, leaderS, locationS, random, resourceS, successS)

fun Aggregate<Int>.fixedRootStability(
    devSpawn: DeviceSpawn,
    distanceS: DistanceSensor,
    env: EnvironmentVariables,
    leaderS: LeaderSensor,
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
        data class Stability(val spawnStable: Boolean = false, val destroyStable: Boolean = false) {
            infix fun and(other: Stability): Boolean = spawnStable && other.spawnStable && destroyStable && other.destroyStable
        }
        share(Stability()) { neighborhoodStability ->
            val lastChanged = evolve(now to listOf(potential, localSuccess, success, localResource)) { last ->
                val current = listOf(potential, localSuccess, success, localResource)
                if (current == last.second) { last } else { now to current }
            }.first
            val enoughTime = now > lastChanged + devSpawn.minSpawnWait
            val everyoneIsStable = neighborhoodStability.fold(enoughTime) { acc, change -> acc and change.destroyStable && change.spawnStable }
            val everyoneIsDestroyStable = neighborhoodStability.fold(lastChanged != now) { acc, change -> acc and change.destroyStable }
            env["enough-time"] = enoughTime
            env["everyone-is-stable"] = everyoneIsStable
            env["everyone-is-destroy-stable"] = everyoneIsDestroyStable
            val localStability = neighborhoodStability.localValue
            when {
                potential > 0.0 && childrenCount == 0 && localResource < resourceS.resourceLowerBound && everyoneIsDestroyStable -> {
                    devSpawn.selfDestroy()
                    Stability(spawnStable = false, destroyStable = false)
                }
                neighborPositions.isEmpty() || localResource / (2 + childrenCount) > resourceS.resourceLowerBound && childrenCount < devSpawn.maxChildren && everyoneIsStable -> {
                    val relativePositions = neighborPositions.map { it - localPosition }
                    val angles = relativePositions.map { atan2(it.second, it.first) }.sorted()
                    fun relativeAngleTowards(center: Double) = PI * random.nextGaussian() / devSpawn.maxChildren + center
                    val angle = when {
                        angles.isEmpty() -> random.nextRandomDouble(0.0..2 * PI)
                        angles.size == 1 -> relativeAngleTowards(angles.first() + PI)
                        else -> {
                            val fullCircle = angles + (angles.first() + 2 * PI)
                            data class Angle(val from: Double, val arc: Double) : Comparable<Angle> {
                                override fun compareTo(other: Angle): Int =
                                    compareBy(Angle::arc).thenBy(Angle::from).compare(this, other)
                            }
                            val minArc = 2 * PI / devSpawn.maxChildren
                            val differences = fullCircle
                                .zipWithNext { a, b -> Angle(a, b - a) }
                                .filter { it.arc >= minArc }
                            when {
                                differences.isEmpty() -> Double.NaN
                                else -> {
                                    val selectedAngle = differences.randomElementWeighted(random) { arc }
                                    relativeAngleTowards(selectedAngle.arc / 2 + selectedAngle.from)
                                }
                            }
                        }
                    }
                    when {
                        angle.isNaN() -> Stability(spawnStable = true, destroyStable = true)
                        else -> {
                            val x = devSpawn.cloningRange * cos(angle)
                            val y = devSpawn.cloningRange * sin(angle)
                            val absoluteDestination = localPosition + (x to y)
                            devSpawn.spawn(absoluteDestination)
                            Stability(spawnStable = false, destroyStable = localStability.destroyStable)
                        }
                    }
                }
                else -> Stability(spawnStable = enoughTime, destroyStable = now > lastChanged)
            }
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
    spawner: Spawner
): Double {
    val isLeader = env.get<Boolean>("leader")
    val potential = findPotential(distanceS, isLeader)
    val localSuccess = obtainLocalSuccess(successS)
    val success = convergeSuccess(successS, potential, localSuccess)
    val localResource = spreadResource(env, potential, success, resourceS.maxResource)
    spawner(devSpawn, locationS, potential, localSuccess, success, localResource)
    return localResource
}