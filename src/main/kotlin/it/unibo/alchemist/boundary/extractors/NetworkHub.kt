package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time

class NetworkHub() : Extractor<Pair<Double, Double>> {
    override val columnNames: List<String>
        get() = listOf<String>(NAME)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Pair<Double, Double>> {
        val sum = environment.fold(0.0 to 0.0) { acc, next ->
            val nodePos = environment.getPosition(next)
            acc.first + nodePos.coordinates[0] to acc.second + nodePos.coordinates[1]
        }
        val center = sum.first / environment.nodeCount to sum.second / environment.nodeCount
        return mapOf(NAME to center)
    }

    private companion object {
        private const val NAME: String = "network-hub"
    }
}