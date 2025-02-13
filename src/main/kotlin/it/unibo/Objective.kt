package it.unibo

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.util.Environments.networkDiameterByHopDistance
import it.unibo.common.DataRetriever.meanOnCleanedData
import java.nio.file.Paths
import kotlin.Double.Companion.NaN
import kotlin.math.pow

fun geometricMean(metrics: Collection<Double>): Double = metrics.fold(1.0) { acc, next -> acc * next.pow(2) }.pow(1.0 / metrics.size)

fun goal(): Double {
    val metrics = listOf(
        "classic-vmc@network-hub-xCoord",
        "classic-vmc@network-hub-yCoord",
        "classic-vmc@network-diameter[mean]",
//        "classic-vmc@network-density[mean]",
        "classic-vmc@network-degree[mean]",
    )
    return geometricMean(
        meanOnCleanedData(listOf("classic-vmc"), Paths.get("").toAbsolutePath().toString() + "/data")
            .filterKeys { it in metrics }.values,
    )
}

class Goal : (Environment<*, *>) -> Double {
    override fun invoke(env: Environment<*, *>): Double = env.minimize()
}

fun Environment<*, *>.minimize(): Double =
    networkHub().let { (xCoord, yCoord) ->
        val current =
            geometricMean(
                listOf(
                    networkDiameterByHopDistance(),
                    networkDegree(),
                    nodeCount.toDouble(),
                    xCoord,
                    yCoord,
                ),
            )
        (goal() - current).mod(2.0)
    }

fun <T> Environment<T, *>.networkHub(): Pair<Double, Double> =
    fold(0.0 to 0.0) { acc, next ->
        val nodePos = this.getPosition(next)
        acc.first + nodePos.coordinates[0] to acc.second + nodePos.coordinates[1]
    }.let { sum ->
        sum.first / this.nodeCount to sum.second / this.nodeCount
    }

fun <T> Environment<T, *>.networkDensity(): Double {
    var outers: MutableMap<String, Double> =
        listOf<String>("top", "bottom", "right", "left")
            .associateWith { NaN }
            .toMutableMap()
    return nodes
        .map { n ->
            val nodePos = getPosition(n).coordinates
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
        }.average()
}

fun <T> Environment<T, *>.networkDegree(): Double =
    nodes
        .map { n ->
            val neighbors = getNeighborhood(n)
            neighbors.size().toDouble()
        }.average()
