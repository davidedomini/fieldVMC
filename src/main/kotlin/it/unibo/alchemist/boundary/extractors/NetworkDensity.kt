package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.ExportFilter
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.util.StatUtil
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import kotlin.Double.Companion.NaN

class NetworkDensity
    @JvmOverloads
    constructor(
        private val filter: ExportFilter,
        aggregators: List<String>,
        precision: Int = 2,
    ) : AbstractAggregatingDoubleExporter(filter, aggregators, precision) {
        private companion object {
            private const val NAME: String = "network-density"
        }

        override val columnName: String = NAME

        private val aggregators: Map<String, UnivariateStatistic> =
            aggregators
                .associateWith { StatUtil.makeUnivariateStatistic(it) }
                .filter { it.value.isPresent }
                .map { it.key to it.value.get() }
                .toMap()

        override val columnNames: List<String> =
            this@NetworkDensity
                .aggregators.keys
                .takeIf { it.isNotEmpty() }
                ?.map { "$NAME[$it]" }
                ?: listOf("$NAME@node-id")

        override fun <T> getData(
            environment: Environment<T, *>,
            reaction: Actionable<T>?,
            time: Time,
            step: Long,
        ): Map<Node<T>, Double> {
            val outers: MutableMap<String, Double> =
                listOf<String>("top", "bottom", "right", "left")
                    .associateWith { NaN }
                    .toMutableMap()
            return environment.nodes.associateWith { n ->
                val nodePos = environment.getPosition(n).coordinates
                val right = outers["right"]!!
                val left = outers["left"]!!
                val top = outers["top"]!!
                val bottom = outers["bottom"]!!
                when {
                    right.isNaN() || nodePos[0] > right -> outers["right"] = nodePos[0]
                    left.isNaN() || nodePos[0] < left -> outers["left"] = nodePos[0]
                    top.isNaN() || nodePos[1] > top -> outers["top"] = nodePos[1]
                    bottom.isNaN() || nodePos[1] < bottom -> outers["bottom"] = nodePos[1]
                    else -> {
                    }
                }

                // Calculate the area of the rectangle given by the outermost nodes
                val area = (outers["right"]!! - outers["left"]!!) * (outers["top"]!! - outers["bottom"]!!)
                environment.nodeCount / area
            }
        }
    }
