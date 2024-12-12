package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.alchemist.device.properties.impl.ExecutionClockProperty
import it.unibo.collektive.alchemist.device.sensors.impl.ResourceSensorProperty

class ResourceDistribution<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    val clock: ExecutionClockProperty<T, P>,
    val resourceSensor: ResourceSensorProperty<T, P>,
    val constConsumptionRate: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> = ResourceDistribution(environment, node, clock, resourceSensor, constConsumptionRate)

    override fun execute() {
        //todo check node's turn
        if(node.getConcentration(SimpleMolecule("leader")) == true) {
            node.setConcentration(SimpleMolecule("resource"), resourceSensor.maxResource as T)
        }
        val children = environment.getNeighborhood(node).filter { n ->
            n.getConcentration(SimpleMolecule("parent")) == node.id
        }
        val weightSum = children.sumOf { n ->
            n.getConcentration(SimpleMolecule("weight")) as Double
        }
        val availableResources = node.getConcentration(SimpleMolecule("resource")) as Double
        children.forEach { n ->
            val weight = n.getConcentration(SimpleMolecule("weight")) as Double
            val resource = (availableResources - constConsumptionRate) * (weight / weightSum)
            n.setConcentration(SimpleMolecule("resource"), resource as T)
        }
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD
}