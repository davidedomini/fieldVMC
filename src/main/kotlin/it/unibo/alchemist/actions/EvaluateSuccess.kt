package it.unibo.alchemist.actions

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.Node.Companion.asProperty
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.alchemist.device.properties.Clock
import it.unibo.collektive.alchemist.device.properties.impl.ExecutionClockProperty
import it.unibo.collektive.alchemist.device.sensors.impl.SuccessSensorProperty
import kotlin.math.max
import kotlin.math.min

class EvaluateSuccess<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    val clock: ExecutionClockProperty<T, P>,
    private val successSensor: SuccessSensorProperty<T, P>,
    val constProductionRate: Double,
    val constTransferRate: Double,
    val sensorProductionRate: Double,
    val sensorTransferRate: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> = EvaluateSuccess(environment, node, clock, successSensor, constProductionRate, constTransferRate, sensorProductionRate, sensorTransferRate)

    override fun execute() {
        // todo add check on the turn of the node
//        val clock = node.properties.first { it is ClockProperty<*, *> }
//        val nbr = environment.getNeighborhood(node).map { n -> n.asProperty() }

//        clock.currentClock()
        if(node.getConcentration(SimpleMolecule("leaf")) == true) {
            val localProduction = max(0.0, production())
            successSensor.setLocalSuccess(localProduction)
            successSensor.setSuccess(localProduction)
        } else if(node.getConcentration(SimpleMolecule("intermediate")) == true) {
            val childrenSuccessSum = environment.getNeighborhood(node).filter { n ->
                n.getConcentration(SimpleMolecule("parent")) == node.id
            }.sumOf { n ->
                n.getConcentration(SimpleMolecule("success")) as Double
            }
            val success = max(0.0, min(1.0, transfer() * childrenSuccessSum))
            successSensor.setSuccess(success)
        } else {
            throw IllegalStateException("Node is not a leaf or an intermediate node")
        }
    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD

    private fun production(): Double =
        constProductionRate + sensorProductionRate * successSensor.getLocalSuccess()

    private fun transfer(): Double =
        constTransferRate + sensorTransferRate * successSensor.getLocalSuccess()
}