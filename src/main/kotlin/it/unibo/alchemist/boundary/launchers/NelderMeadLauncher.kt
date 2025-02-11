package it.unibo.alchemist.boundary.launchers

import it.unibo.alchemist.boundary.Launcher
import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.Variable
import it.unibo.common.DataRetriever.retrieveData
import java.nio.file.Paths
import kotlin.math.pow

class NelderMeadLauncher
@JvmOverloads
constructor(
    private val variables: List<String> = emptyList(),
    private val metrics: List<String> = emptyList(),
    private val maxBatchSize: Double = 0.0,
    private val autostart: Boolean = false,
    private val showProgress: Boolean = false,
    private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
) : Launcher {
    companion object {
        val experiments = listOf("classic-vmc", "fixed-leader")
        val path = Paths.get("").toAbsolutePath().toString() + "/data"
        private fun geometricMean(metrics: Collection<Double>): Double =
            metrics.fold(1.0) { acc, next -> acc * next }.pow(1.0 / metrics.size)

    }

    val objFunctionClassicVMC =
        geometricMean(
            retrieveData(
                listOf(experiments.first()),
                path,
            ).filter { (key, _) -> key.removePrefix("${experiments.first()}@") in metrics }
                .filterNot { (_, value) -> value.isNaN() || value <= 0 }
                .values,
        )

    @Synchronized
    override fun launch(loader: Loader) {
        val instances: Map<String, Variable<Double>> = loader.variables.filter { it.key in variables }.map { it.key to it.value as Variable<Double> }.toMap()
        if (instances.isEmpty()) {
            throw IllegalArgumentException("No matching variables found.")
        }
        // I want to fill the initial symplex variables.size + 1 times,
        // so that I can have a symplex with variables.size + 1 vertices
        // each vertex will have a different value for each variable,
        // if the iteration is the first, I take the min value for each variable
        // if the iteration is the last, I take the max value for each variable
        // if the iteration is in between, I take the default value for each variable
        // otherwise, I take the value of the variable at the index of the iteration
        // variables can have different lengths
        val keys = instances.keys.toList()
        val variablesToKey = keys.map { key -> instances.getValue(key) }
        val minMax: List<Pair<Double, Double>> = variablesToKey.map { variable ->
            val max = variable.stream().toList().max()
            val min = variable.stream().toList().min()
            (min to max)
        }
        val allMin = minMax.map { it.first }
        val allMax = minMax.map { it.second }
        // generate all solution which mix min and max values
        val simplexVertex = mutableListOf<List<Double>>(allMin)
        for (i in 0 until variables.size) {
            val left = allMin.subList(0, i)
            val right = allMax.subList(i + 1, allMax.size)
            simplexVertex.add(left + allMax[i] + right)
        }
    }

    private fun goalFunction(objective: Double): (Collection<Double>) -> Double = { toOptimize ->
        (objective - geometricMean(toOptimize)).mod(2.0)
    }
}