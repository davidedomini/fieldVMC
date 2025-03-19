package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.ExportFilter
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.util.Environments.allSubNetworksByNode

class SubnetworksDiameter
@JvmOverloads
constructor(
    filter: ExportFilter,
    aggregators: List<String>,
    precision: Int = 2,
) : AbstractAggregatingDoubleExporter(filter, aggregators, precision) {
    private companion object {
        private const val NAME: String = "subnetworks-diameter"
        private operator fun Node<*>.get(name: String) = getConcentration(SimpleMolecule(name))
        private val Node<*>.parent: Int
            get() = when (val parent: Any? = get("parent")) {
                is Int -> parent
                is Number -> parent.toInt()
                is String -> parent.toInt()
                else -> error("Parent value not yet supported.")
            }
    }

    override val columnName: String = NAME

    override fun <T> getData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<Node<T>, Double> =
        environment
            .allSubNetworksByNode { n1, n2 -> if (n1.parent == n2.id) 1.0 else 0.0 }
            .mapValues { (_, subnet) -> subnet.diameter }
}
