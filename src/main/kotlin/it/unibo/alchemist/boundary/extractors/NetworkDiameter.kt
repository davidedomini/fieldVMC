package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.ExportFilter
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.util.Environments.networkDiameterByHopDistance

class NetworkDiameter
    @JvmOverloads
    constructor(
        filter: ExportFilter,
        aggregators: List<String>,
        precision: Int = 2,
    ) : AbstractAggregatingDoubleExporter(filter, aggregators, precision) {
        private companion object {
            private const val NAME: String = "network-diameter"
        }

        override val columnName: String = NAME

        override fun <T> getData(
            environment: Environment<T, *>,
            reaction: Actionable<T>?,
            time: Time,
            step: Long,
        ): Map<Node<T>, Double> =
            environment.networkDiameterByHopDistance().let { diameter ->
                environment.nodes.associateWith { diameter }
            }
    }
