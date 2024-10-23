package it.unibo.collektive.coordination

import it.unibo.alchemist.collektive.device.DistanceSensor
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.field.Field
import it.unibo.collektive.field.Field.Companion.fold

/**
 * Elect the leader in an area limited by the [radius], based on the [localStrength] of the node.
 */
context(DistanceSensor)
fun <ID : Any, C : Comparable<C>> Aggregate<ID>.boundedElection(
    localStrength: C,
    radius: Double,
): ID {
    data class Candidacy<ID : Any>(val strength: C, val distance: Double, val leaderId: ID) : Comparable<Candidacy<ID>> {
        override fun compareTo(other: Candidacy<ID>): Int =
            Comparator<Candidacy<ID>> { a, b -> b.strength.compareTo(a.strength) }
                .thenBy { it.distance }
                .thenBy {
                    when (it.leaderId) {
                        is Comparable<*> -> it.leaderId
                        else -> 0
                    }
                }
                .compare(this, other)
    }
    val local: Candidacy<ID> = Candidacy(localStrength, 0.0, localId)
    return share(local) { candidates ->
        val candidate = candidates
            .alignedMap(distances()) { c, m -> Candidacy(c.strength, c.distance + m, c.leaderId) }
        val field: Field<ID, Candidacy<ID>?> = candidate
            .mapWithId { id, c -> c.takeUnless { id == localId || it.distance > radius } }
        field.fold(local) { accumulator, newValue ->
            when {
                newValue == null -> accumulator
                else -> minOf(accumulator, newValue)
            }
        }
    }.leaderId
}
