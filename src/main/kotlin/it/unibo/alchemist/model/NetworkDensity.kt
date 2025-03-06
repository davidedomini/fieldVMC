package it.unibo.alchemist.model

import kotlin.math.max
import kotlin.math.min

object NetworkDensity {

    fun <T : Any> Environment<T, *>.networkDensity(): Double {
        data class BoundingBox(
            val minX: Double = Double.Companion.POSITIVE_INFINITY,
            val maxX: Double = Double.Companion.NEGATIVE_INFINITY,
            val minY: Double = Double.Companion.POSITIVE_INFINITY,
            val maxY: Double = Double.Companion.NEGATIVE_INFINITY,
        )

        val boundingBox = this.fold(BoundingBox()) { bb, node ->
            val (x, y) = getPosition(node).coordinates
            BoundingBox(
                min(x, bb.minX),
                max(x, bb.maxX),
                min(y, bb.minY),
                max(y, bb.maxY),
            )
        }
        val area = (boundingBox.maxX - boundingBox.minX) * (boundingBox.maxY - boundingBox.minY)
        if (area <= 0 || area.isInfinite()) return Double.Companion.NaN
        return nodeCount / area
    }
}