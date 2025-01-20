package it.unibo.collektive.lib

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.SuccessSensor
import it.unibo.collektive.coordination.convergeCast

/**
 * Converge the success of the children nodes to itself.
 */
fun <ID : Comparable<ID>> Aggregate<ID>.convergeSuccess(
    successSensor: SuccessSensor,
    potential: Double,
    localSuccess: Double,
): Double = convergeCast(potential, localSuccess) { a, b -> a + b }.also { successSensor.setSuccess(it) }

/**
 * Get the local success of the node.
 */
fun obtainLocalSuccess(successSensor: SuccessSensor): Double = successSensor.getLocalSuccess().also { successSensor.setLocalSuccess(it) }
