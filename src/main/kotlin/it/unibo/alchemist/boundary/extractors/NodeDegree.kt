package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.ExportFilter
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule

class NodeDegree
    @JvmOverloads
    constructor(
        filter: ExportFilter,
        aggregators: List<String>,
        private val checkChildren: Boolean = false,
        precision: Int = 2,
    ) : AbstractAggregatingDoubleExporter(filter, aggregators, precision) {
        private companion object {
            private const val NAME = "nodes-degree"
        }

        override val columnName: String = NAME

        override fun <T> getData(
            environment: Environment<T, *>,
            reaction: Actionable<T>?,
            time: Time,
            step: Long,
        ): Map<Node<T>, Double> =
            environment.nodes.associateWith { node ->
                val neighbors = environment.getNeighborhood(node)
                if (checkChildren) {
                    neighbors
                        .filter { n ->
                            n.getConcentration(SimpleMolecule("parent")) == node.id ||
                                node.getConcentration(SimpleMolecule("parent")) == n.id
                        }.size
                } else {
                    neighbors.size()
                }.toDouble()
            }
    }
