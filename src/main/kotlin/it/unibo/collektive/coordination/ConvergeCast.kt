package it.unibo.collektive.coordination

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.field.Field
import it.unibo.collektive.field.Field.Companion.fold
import it.unibo.collektive.field.Field.Companion.hood
import it.unibo.collektive.field.min

// A field mapping input channels to this device to the value channelled in
data class Channel<T>(val isFromChild: Boolean, val localValue: T)

/**
 * Aggregate a field of type T within a spanning tree built according to the maximum
 * decrease in [potential].
 * Accumulate the [potential] according to the [reduce] function.
 * [local] is the value field providing the value to be collected for each device.
 */
fun <T, ID : Comparable<ID>> Aggregate<ID>.convergeCast(
    potential: Double,
    local: T,
    disambiguateParent: (ID, ID) -> ID = { a, b -> minOf(a, b) },
    reduce: (T, T) -> T,
): T = share(local) { field ->
    val parent = findParent(potential, disambiguateParent)
    val neighborParents = neighboring(parent) // Each device is mapped to its parent
    val childrenValues = neighborParents.alignedMap(field) { itsParent, itsLocal ->
        Channel(isFromChild = itsParent == localId, itsLocal)
    }
    childrenValues.fold(local) { accumulator, channel ->
        if (channel.isFromChild) reduce(accumulator, channel.localValue) else accumulator
    }
}

/**
 * Spreads the [localResource] to the children of this node, according to the [localSuccess] of each child.
 */
context(EnvironmentVariables)
fun <ID : Comparable<ID>> Aggregate<ID>.spreadToChildren(
    potential: Double,
    localResource: Double,
    localSuccess: Double,
    disambiguateParent: (ID, ID) -> ID = { a, b -> minOf(a, b) },
): Double = exchanging(localResource) { resource ->
    val parent = findParent(potential, disambiguateParent) // the parent of this device
    val myLocalResources = resource.mapWithId { id, neighborResource ->
        if (id == parent) neighborResource else 0.0
    }.fold(localResource) { a, b -> a + b }
    val neighborParents = neighboring(parent) // Each device is mapped to its parent
    val childrenSuccess: Field<ID, Double> = neighborParents
        .alignedMap(neighboring(localSuccess)) { itsParent, itsSuccess ->
            when {
                itsParent == localId -> itsSuccess
                else -> 0.0
            }
        }
    val selfConsumption = myLocalResources / childrenSuccess.fold(1) { a, b -> a + (1.takeIf { b > 0 } ?: 0) }
    if (potential > 0.0) set("resource", selfConsumption)
    val resourcesToSpread = myLocalResources - selfConsumption
    val overallChildrenSuccess = childrenSuccess.hood(Double.NEGATIVE_INFINITY) { a, b -> a + b }
    childrenSuccess.map { if (overallChildrenSuccess <= 0) 0.0 else it * resourcesToSpread / overallChildrenSuccess }
        .yielding {
            neighboring(myLocalResources)
        }
}.localValue

/**
 * Finds the parent of this node in the spanning tree built according to the maximum decrease in [potential].
 */
fun <ID : Comparable<ID>> Aggregate<ID>.findParent(
    potential: Double,
    disambiguateParent: (ID, ID) -> ID = { a, b -> minOf(a, b) },
): ID {
    val neighboringPotential = neighboring(potential)
    val localMinimum = neighboringPotential.min(potential)
    return neighboringPotential.asSequence()
        .filter { (_, v) -> v == localMinimum }
        .map { it.first }
        .reduce(disambiguateParent) // the parent
}
