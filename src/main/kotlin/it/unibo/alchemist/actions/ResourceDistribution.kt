package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction

class ResourceDistribution<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> = ResourceDistribution(environment, node)

    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD

}