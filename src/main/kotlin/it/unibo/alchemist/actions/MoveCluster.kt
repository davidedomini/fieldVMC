package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction

class MoveCluster<T : Any, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    private val movingRange: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(node: Node<T>, reaction: Reaction<T>): Action<T> =
        MoveCluster(environment, node, movingRange)

    override fun execute() {
        val nodePosition = environment.getPosition(node).coordinates
        val newPosition = (nodePosition[0] + movingRange) to nodePosition[1]
        environment.moveNodeToPosition(node, environment.makePosition(newPosition.first, newPosition.second))
    }

    override fun getContext(): Context = Context.NEIGHBORHOOD
}
