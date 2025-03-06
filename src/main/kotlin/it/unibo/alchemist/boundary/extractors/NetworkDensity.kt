package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.max
import kotlin.math.min

class NetworkDensity() : Extractor<Double> {
    private companion object {
        private const val NAME: String = "network-density"
    }

    override val columnNames: List<String> = listOf(NAME)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        data class BoundingBox(
            val minX: Double = POSITIVE_INFINITY,
            val maxX: Double = NEGATIVE_INFINITY,
            val minY: Double = POSITIVE_INFINITY,
            val maxY: Double = NEGATIVE_INFINITY,
        )
        val boundingBox = environment.fold(BoundingBox()) { bb, node ->
            val (x, y) = environment.getPosition(node).coordinates
            BoundingBox(
                min(x, bb.minX),
                max(x, bb.maxX),
                min(y, bb.minY),
                max(y, bb.maxY),
            )
        }
        val area = (boundingBox.maxX - boundingBox.minX) * (boundingBox.maxY - boundingBox.minY)
        return mapOf(NAME to (environment.nodeCount / area))
    }
}
