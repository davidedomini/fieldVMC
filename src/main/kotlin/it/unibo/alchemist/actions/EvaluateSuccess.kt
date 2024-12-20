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
import it.unibo.collektive.alchemist.device.properties.Cycle.SPAWNING
import it.unibo.collektive.alchemist.device.properties.impl.ExecutionClockProperty
import it.unibo.collektive.alchemist.device.sensors.impl.SuccessSensorProperty
import kotlin.math.max
import kotlin.math.min

class EvaluateSuccess<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    private val successSensor: SuccessSensorProperty<T, P>,
    private val constProductionRate: Double,
    private val constTransferRate: Double,
    private val sensorProductionRate: Double,
    private val sensorTransferRate: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> =
        EvaluateSuccess(
            environment,
            node,
            node.asProperty<T, SuccessSensorProperty<T, P>>(),
            constProductionRate,
            constTransferRate,
            sensorProductionRate,
            sensorTransferRate,
        )

    override fun execute() {
        val allNodes = environment.nodes.map { it to it.asProperty<T, ExecutionClockProperty<T, P>>() }
        val current = node.asProperty<T, ExecutionClockProperty<T, P>>().currentClock()
        val children = allNodes
            .filterNot { (n, _) -> n.id == node.id }
            .filter { (n, _) -> n.getConcentration(SimpleMolecule("parent")) == node.id }
        val childrenNotInBackward = children.filterNot { (_, c) ->
            c.currentClock() == Clock(time = current.time + 1, action = BACKWARD)
        }
        val parent = allNodes
            .filterNot { (n, _) -> n.id == node.id }
            .firstOrNull { (n, _) -> node.getConcentration(SimpleMolecule("parent")) == n.id }
        val leaves = allNodes.filterNot { (n, _) ->  }
        val isLeaf = allNodes
            .filterNot { (n, _) -> n.id == node.id } // root can be a leaf
            .none { (n, _) -> n.getConcentration(SimpleMolecule("parent")) == node.id }
        if( (parent == null && isLeaf) // if root is a leaf
            || (isLeaf && (current.action == SPAWNING || current.action == FORWARD)) ){ // if the leaf is in forward or just has spawned
            val localProduction = max(0.0, production())
            successSensor.setLocalSuccess(localProduction)
            successSensor.setSuccess(localProduction)
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        } else if (children.isNotEmpty() && current.action == FORWARD && childrenNotInBackward.isEmpty()) { // if it has children and it is in forward and its children are in backward
            val childrenSuccessSum =
                children.sumOf { (n, _) ->
                    n.getConcentration(SimpleMolecule("success")) as Double
                }
            val success = max(0.0, min(1.0, transfer() * childrenSuccessSum))
            node.setConcentration(SimpleMolecule("success"), success as T)
            node.asProperty<T, ExecutionClockProperty<T, P>>().nextClock()
        }
    }

    private fun getClockFromProperty(node: Node<T>): Clock {
        return node.asProperty<T, ExecutionClockProperty<T, P>>().currentClock()
    }
    override fun getContext(): Context? = Context.NEIGHBORHOOD

    private fun production(): Double = constProductionRate + sensorProductionRate * successSensor.getLocalSuccess()

    private fun transfer(): Double = constTransferRate + sensorTransferRate * successSensor.getLocalSuccess()
}
