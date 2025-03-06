package it.unibo.common

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.util.Environments.networkDiameterByHopDistance
import it.unibo.common.NetworkMetrics.networkDensity
import it.unibo.common.NetworkMetrics.networkHub
import it.unibo.common.NetworkMetrics.nodesDegree

class TerminationMetrics : (Environment<*, *>) -> Map<String, Double> {
    override fun invoke(env: Environment<*, *>): Map<String, Double> =
        env.networkHub().let { (xCoord, yCoord) ->
            mapOf(
                "nodes" to env.nodeCount.toDouble(),
                "network-hub-xCoord" to xCoord,
                "network-hub-yCoord" to yCoord,
                "network-density" to env.networkDensity(),
                "network-diameter" to env.networkDiameterByHopDistance(),
                "nodes-degree[mean]" to env.nodesDegree().average(),
            )
        }
}