package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction

class RemoveNodes<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    private val killingRange: Double,
    private val origin: Double,
    private val width: Double,
    private val height: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(node: Node<T>, reaction: Reaction<T>): Action<T> =
        RemoveNodes(environment, node, killingRange, origin, width, height)

    override fun execute() {
        val nodePosition = environment.getPosition(node).coordinates
        val start = (width / 2) - (killingRange / 2) to origin
        val end = (width / 2) + (killingRange / 2) to height
        if (nodePosition[0] in start.first..end.first && nodePosition[1] in start.second..end.second) {
            environment.removeNode(node)
        }
    }

    override fun getContext(): Context = Context.NEIGHBORHOOD
}
