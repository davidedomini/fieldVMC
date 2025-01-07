package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Node.Companion.asProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.alchemist.device.properties.Cycle.FORWARD
import it.unibo.collektive.alchemist.device.properties.Cycle.MAX
import it.unibo.collektive.alchemist.device.properties.impl.ExecutionClockProperty
import it.unibo.collektive.alchemist.device.sensors.impl.ResourceSensorProperty

class FindMax<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    private val resourceSensor: ResourceSensorProperty<T, P>,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> = FindMax(environment, node, resourceSensor)

    override fun execute() {
        val neighbors = environment.getNeighborhood(node)
            .filterNot { n -> n.id == node.id }
            .map { it to it.asProperty<T, ExecutionClockProperty<T, P>>() }
        val current = node.asProperty<T, ExecutionClockProperty<T, P>>().currentClock()
        val parent = neighbors.firstOrNull { (n, _) -> node.getConcentration(SimpleMolecule("parent")) == n.id } // if null then it is root
        val children = neighbors.filter { (n, _) -> n.getConcentration(SimpleMolecule("parent")) == node.id }
        val childrenNotDone = children.filterNot { (_, c) -> c.currentClock().action == MAX }
        if ((children.isEmpty() && parent == null ) || // if I am leaf and root I should spawn anyway
            (children.isEmpty() && current.action == FORWARD && parent != null && parent.second.currentClock().action == FORWARD) ) { // I'm leaf and I think I'm max
            node.setConcentration(SimpleMolecule("max-leaf-id"), node.id as T)
            node.setConcentration(SimpleMolecule("max-leaf-res"), node.getConcentration(SimpleMolecule("resource")))
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        } else if (children.isNotEmpty() && childrenNotDone.isEmpty() && current.action == FORWARD ) {
            val maxChildren = children
                .maxBy { (n, _) -> n.getConcentration(SimpleMolecule("max-leaf-res")) as Double }.first
            node.setConcentration(SimpleMolecule("max-leaf-id"), maxChildren.getConcentration(SimpleMolecule("max-leaf-id")))
            node.setConcentration(SimpleMolecule("max-leaf-res"), maxChildren.getConcentration(SimpleMolecule("max-leaf-res")))
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        }
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD
}