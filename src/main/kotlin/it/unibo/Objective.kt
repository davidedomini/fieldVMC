package it.unibo

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.util.Environments.networkDiameterByHopDistance
import it.unibo.common.DataRetriever.meanOnCleanedData
import java.nio.file.Paths
import kotlin.Double.Companion.NaN
import kotlin.math.pow

/**
 * Compute the geometric mean of a collection of [metrics].
 * The geometric mean is the n-th root of the product of the metrics.
 * The n-th root is computed by raising the product to the power of 1/n.
 */
fun geometricMean(metrics: Collection<Double>): Double =
    metrics.fold(1.0) { acc, next -> acc * next }.pow(1.0 / metrics.size)

/**
 * The target value for the optimization.
 * It is the geometric mean of the mean values of the metrics for the classic-vmc experiment.
 * The metrics are:
 * - number of nodes;
 * - x and y coordinates of the network hub;
 * - network diameter;
 * - network degree.
 */
fun target(): Double {
    val metrics =
        listOf(
            "classic-vmc@nodes",
            "classic-vmc@network-hub-xCoord",
            "classic-vmc@network-hub-yCoord",
            "classic-vmc@network-density[max]",
            "classic-vmc@network-diameter[mean]",
            "classic-vmc@nodes-degree[mean]",
        )
    return geometricMean(
        meanOnCleanedData(listOf("classic-vmc"), Paths.get("").toAbsolutePath().toString() + "/data")
            .filterKeys { it in metrics }
            .values,
    )
}

/**
 * The goal for the optimization.
 */
class Goal : (Environment<*, *>) -> Double {
    val target = target()

    override fun invoke(env: Environment<*, *>): Double = env.minimize(target)
}

/**
 * The function to minimize.
 * It is the difference between the target value and the current value.
 * The current value is the geometric mean of the metrics for the given [Environment].
 * The difference is taken modulo 2 to avoid negative values.
 * The metric values are:
 * - number of nodes;
 * - x and y coordinates of the network hub;
 * - network diameter;
 * - network degree.
 */
fun Environment<*, *>.minimize(target: Double): Double =
    networkHub().let { (xCoord, yCoord) ->
        val current =
            geometricMean(
                listOf(
                    nodeCount.toDouble(),
                    xCoord,
                    yCoord,
                    networkDiameterByHopDistance(),
                    networkDensity().max(),
                    nodesDegree(),
                ),
            )
        (target - current).mod(2.0)
    }

/**
 * The network hub coordinates.
 * The x and y coordinates are the average of the x and y coordinates of the nodes in the network.
 */
fun <T> Environment<T, *>.networkHub(): Pair<Double, Double> =
    fold(0.0 to 0.0) { acc, next ->
        val nodePos = this.getPosition(next).coordinates.map { it + 10 } // Add 10 to avoid negative positions
        acc.first + nodePos[0] to acc.second + nodePos[1]
    }.let { sum ->
        sum.first / this.nodeCount to sum.second / this.nodeCount
    }

/**
 * The degree of the nodes in the network.
 * The degree of a node is the number of neighbors it has.
 * The result is the average of the degrees of all the nodes in the network.
 */
fun <T> Environment<T, *>.nodesDegree(): Double =
    nodes
        .map { n ->
            val neighbors = getNeighborhood(n)
            neighbors.size().toDouble()
        }.average()

/**
 * The network density.
 * The network density is the average number of nodes per unit area.
 * The area is the rectangle given by the outermost nodes in the network.
 * The result is the average of the network densities of all the nodes in the network.
 */
fun <T> Environment<T, *>.networkDensity(): List<Double> {
    var outers: MutableMap<String, Double> =
        listOf<String>("top", "bottom", "right", "left")
            .associateWith { NaN }
            .toMutableMap()
    return nodes
        .map { n ->
            val nodePos = getPosition(n).coordinates.map { it + 10 } // Add 10 to avoid negative positions
            outers =
                outers.mapValues { (key, value) ->
                    when {
                        key == "right" && (value.isNaN() || nodePos[0] > value) -> nodePos[0]
                        key == "left" && (value.isNaN() || nodePos[0] < value) -> nodePos[0]
                        key == "top" && (value.isNaN() || nodePos[1] > value) -> nodePos[1]
                        key == "bottom" && (value.isNaN() || nodePos[1] < value) -> nodePos[1]
                        else -> value
                    }
                } as MutableMap<String, Double>
            // Calculate the area of the rectangle given by the outermost nodes
            val area = (outers["right"]!! - outers["left"]!!) * (outers["top"]!! - outers["bottom"]!!)
            nodeCount / area
        }.filterNot { it.isNaN() || it.isInfinite()}
}
