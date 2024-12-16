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
import it.unibo.collektive.alchemist.device.properties.Clock
import it.unibo.collektive.alchemist.device.properties.Cycle.BACKWARD
import it.unibo.collektive.alchemist.device.properties.Cycle.FORWARD
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
        val allNodes = environment.nodes.map { it to it.asProperty<T, ExecutionClockProperty<T, P>>() }
        val parent = allNodes.find { (n, _) -> node.id == n.getConcentration(SimpleMolecule("parent")) }
        val current = clock.currentClock()
        val nodesNotInBackward = allNodes.filterNot { (_, nodesClock) -> nodesClock.currentClock().action == BACKWARD }
        if( (parent == null && nodesNotInBackward.isEmpty()) ||
            (parent!!.second.currentClock() == Clock(time = current.time, action = FORWARD)) ) {
            var availableResources = node.getConcentration(SimpleMolecule("resource")) as Double
            if (parent == null) {
                availableResources = availableResources + resourceSensor.maxResource
            }
            // keep some resources
            val remainingResources = availableResources - constConsumptionRate
            node.setConcentration(SimpleMolecule("resource"), remainingResources as T)
            val children = allNodes.filter { (n, _) ->
                n.getConcentration(SimpleMolecule("parent")) == node.id
            }
            // clock next
            clock.nextClock()
            if (children.isNotEmpty()) {
                // update weight & send resources
                val weightSum = children.sumOf { (n, _) ->
                    n.getConcentration(SimpleMolecule("weight")) as Double
                }
                children.forEach { (n, _) ->
                    // get the weight of the connection between the parent and the child
                    val weight = n.getConcentration(SimpleMolecule("weight")) as Double
                    // get the success of the child
                    val success = n.getConcentration(SimpleMolecule("success")) as Double
                    // update the weight of the connection
                    val newWeight = weight + vesselsAdaptationRate * (success.pow(competition()) - weight)
                    n.setConcentration(SimpleMolecule("weight"), newWeight as T)
                    // update the resource of the child based on the weight of the connection
                    val resource = remainingResources * (weight / weightSum)
                    n.setConcentration(SimpleMolecule("resource"), resource as T)
                }

            }
        }
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD

    private fun competition(): Double = constCompetitionRate + sensorCompetitionRate * successSensor.getLocalSuccess()
}
