package it.unibo.alchemist.model.linkingrules

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.LinkingRule
import it.unibo.alchemist.model.Neighborhood
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position

class ConnectDependentNodes<T, P : Position<P>> : LinkingRule<T, P>  {
    override fun computeNeighborhood(
        node: Node<T?>?,
        environment: Environment<T?, P?>?,
    ): Neighborhood<T?>? {
        TODO("Not yet implemented")
    }

    override fun isLocallyConsistent(): Boolean = true
}