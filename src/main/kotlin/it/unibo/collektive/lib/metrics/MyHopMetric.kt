package it.unibo.collektive.lib.metrics

import it.unibo.alchemist.collektive.device.DistanceSensor
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.field.Field

data class MyHopMetric(val step: Double = 1.0) : DistanceSensor {
    override fun <ID : Any> Aggregate<ID>.distances(): Field<ID, Double> =
        neighboring(step)
            .mapWithId { id, value ->
                if (id == localId) 0.0 else value + step
            }
}
