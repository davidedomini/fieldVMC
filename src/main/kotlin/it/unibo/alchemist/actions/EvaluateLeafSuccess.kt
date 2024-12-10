package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.collektive.alchemist.device.sensors.SuccessSensor
import kotlin.math.max

class EvaluateLeafSuccess<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    val successSensor: SuccessSensor,
    val constProductionRate: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> = EvaluateLeafSuccess(environment, node, successSensor, constProductionRate)

    override fun execute() {
        val prod = max(0.0, production())

    }

    override fun getContext(): Context? = Context.NEIGHBORHOOD

    private fun production() = constProductionRate + successSensor.sensorProductionRate * successSensor.getSuccess()
}