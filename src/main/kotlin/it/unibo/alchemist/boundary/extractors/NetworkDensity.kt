package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import kotlin.Double.Companion.NaN

class NetworkDensity() : Extractor<Double> {
    override val columnNames: List<String>
        get() = listOf(NAME)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Double> {
        val outers: MutableMap<String, Double> =
            listOf<String>("top", "bottom", "right", "left")
                .associateWith { NaN }
                .toMutableMap()
        environment.nodes.forEach { n ->
            val nodePos = environment.getPosition(n).coordinates
            val right = outers["right"]!!
            val left = outers["left"]!!
            val top = outers["top"]!!
            val bottom = outers["bottom"]!!
            when {
                right.isNaN() || nodePos[0] > right -> outers["right"] = nodePos[0]
                left.isNaN() || nodePos[0] < left ->  outers["left"] = nodePos[0]
                top.isNaN() || nodePos[1] > top -> outers["top"] = nodePos[1]
                bottom.isNaN() || nodePos[1] < bottom -> outers["bottom"] = nodePos[1]
                else -> {
                }
            }
        }
        // Calculate the area of the rectangle given by the outermost nodes
        val area = (outers["right"]!! - outers["left"]!!) * (outers["top"]!! - outers["bottom"]!!)
        val density = environment.nodeCount / area
        return mapOf(NAME to density)
    }

    private companion object {
        private const val NAME: String = "network-density"
    }
}