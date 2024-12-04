package it.unibo.collektive.vmc

import it.unibo.collektive.alchemist.device.sensors.RandomGenerator

operator fun Pair<Double, Double>.minus(other: Pair<Double, Double>): Pair<Double, Double> =
    first - other.first to second - other.second
operator fun Pair<Double, Double>.plus(other: Pair<Double, Double>): Pair<Double, Double> =
    first + other.first to second + other.second

fun <T> Iterable<T>.randomElementWeighted(random: RandomGenerator, by: T.() -> Double): T {
    val total = fold(0.0) { acc, element -> acc + by(element) }
    val selector: Double = random.nextRandomDouble(0.0..total)
    var accumulator = 0.0
    for (element in this) {
        accumulator += by(element)
        if (accumulator >= selector) {
            return element
        }
    }
    return last()
}
