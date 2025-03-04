package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule

class NumberOfLeaves : Extractor<Double> {
    private companion object {
        private const val NAME = "number-of-leaves"
    }

    override val columnNames: List<String>
        get() = listOf<String>(NAME)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> =
        environment.nodes.associateWith { node ->
            environment.getNeighborhood(node).filter { n ->
                n.getConcentration(SimpleMolecule("parent")) == node.id
            }.size.toDouble() // if the node has no children, it is a leaf
        }.filter { it.value == 0.0 }.size.toDouble().let {
            mapOf(NAME to it)
        }
}
