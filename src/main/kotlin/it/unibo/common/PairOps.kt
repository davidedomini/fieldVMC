package it.unibo.common

/**
 * Difference between the given [Pair]s and an [other].
 */
operator fun Pair<Double, Double>.minus(other: Pair<Double, Double>): Pair<Double, Double> =
    first - other.first to second - other.second

/**
 * Sum of the given [Pair]s and an [other].
 */
operator fun Pair<Double, Double>.plus(other: Pair<Double, Double>): Pair<Double, Double> = first + other.first to second + other.second
