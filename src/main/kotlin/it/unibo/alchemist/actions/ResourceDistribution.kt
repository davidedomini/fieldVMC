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
import it.unibo.collektive.alchemist.device.sensors.impl.SuccessSensorProperty
import kotlin.math.pow

class ResourceDistribution<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    private val clock: ExecutionClockProperty<T, P>,
    private val resourceSensor: ResourceSensorProperty<T, P>,
    private val successSensor: SuccessSensorProperty<T, P>,
    private val constConsumptionRate: Double,
    private val constCompetitionRate: Double,
    private val sensorCompetitionRate: Double,
    private val vesselsAdaptationRate: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> =
        ResourceDistribution(
            environment,
            node,
            clock,
            resourceSensor,
            successSensor,
            constConsumptionRate,
            constCompetitionRate,
            sensorCompetitionRate,
            vesselsAdaptationRate,
        )

    override fun execute() {
        // todo check node's turn
        if (node.getConcentration(SimpleMolecule("leader")) == true) {
            val rootResource = node.getConcentration(SimpleMolecule("resource")) as Double
            node.setConcentration(SimpleMolecule("resource"), (rootResource + resourceSensor.maxResource) as T)
        }
        val children = environment.getNeighborhood(node).filter { n ->
            n.getConcentration(SimpleMolecule("parent")) == node.id
        }
        val weightSum = children.sumOf { n ->
            n.getConcentration(SimpleMolecule("weight")) as Double
        }
        val availableResources = node.getConcentration(SimpleMolecule("resource")) as Double
        children.forEach { n ->
            // get the weight of the connection between the parent and the child
            val weight = n.getConcentration(SimpleMolecule("weight")) as Double
            // get the success of the child
            val success = n.getConcentration(SimpleMolecule("success")) as Double
            // update the weight of the connection
            val newWeight = weight + vesselsAdaptationRate * (success.pow(competition()) - weight)
            n.setConcentration(SimpleMolecule("weight"), newWeight as T)
            // update the resource of the child based on the weight of the connection
            val resource = (availableResources - constConsumptionRate) * (weight / weightSum)
            n.setConcentration(SimpleMolecule("resource"), resource as T)
        }
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD

    private fun competition(): Double = constCompetitionRate + sensorCompetitionRate * successSensor.getLocalSuccess()
}
