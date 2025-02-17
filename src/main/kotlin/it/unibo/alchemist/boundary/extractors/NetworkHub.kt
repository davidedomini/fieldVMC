package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time

class NetworkHub : Extractor<Double> {
    private companion object {
        private const val NAME: String = "network-hub"
    }

    override val columnNames: List<String>
        get() = listOf<String>("$NAME-xCoord", "$NAME-yCoord")

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        val sum =
            environment.fold(0.0 to 0.0) { acc, next ->
                // Add 10 to avoid negative positions
                val nodePos = environment.getPosition(next).coordinates.map { it + 10 }
                acc.first + nodePos[0] to acc.second + nodePos[1]
            }
        val center = sum.first / environment.nodeCount to sum.second / environment.nodeCount
        return center.toList().mapIndexed { index, value -> "$NAME@$index" to value }.toMap()
    }
}
