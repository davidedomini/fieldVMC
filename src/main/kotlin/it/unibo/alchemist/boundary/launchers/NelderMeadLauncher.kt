package it.unibo.alchemist.boundary.launchers

import it.unibo.alchemist.boundary.Launcher
import it.unibo.alchemist.boundary.Loader
import it.unibo.common.DataRetriever.meanOnCleanedData
import org.danilopianini.rrmxmx.RrmxmxRandom
import java.nio.file.Paths
import kotlin.math.pow

class NelderMeadLauncher
@JvmOverloads
constructor(
    private val variables: List<String> = emptyList(),
    private val metrics: List<String> = emptyList(),
    private val maxBatchSize: Double = 0.0,
    private val autostart: Boolean = false,
    private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
    private val showProgress: Boolean = false,
    private val seed: ULong = RrmxmxRandom.DEFAULT_SEED,
) : Launcher {
    companion object {
        val experiments = listOf("classic-vmc", "fixed-leader")
        val path = Paths.get("").toAbsolutePath().toString() + "/data"
        private fun geometricMean(metrics: Collection<Double>): Double =
            metrics.fold(1.0) { acc, next -> acc * next }.pow(1.0 / metrics.size)

    }

    val objFunctionClassicVMC =
        geometricMean(
            meanOnCleanedData(
                listOf(experiments.first()),
                path,
            ).filter { (key, _) -> key.removePrefix("${experiments.first()}@") in metrics }
                .filterNot { (_, value) -> value.isNaN() || value <= 0 }
                .values,
        )

    @Synchronized
    override fun launch(loader: Loader) {
        require(loader.variables.isNotEmpty() || variables.isNotEmpty()) {
            "No variables found, can not optimize anything."
        }
        // I need that the variables passed through the loader are numbers
        val randomGenerator = RrmxmxRandom(seed)
        val instances: Map<String, ClosedRange<Double>> = variables.associateWith { varName ->
            val variable = loader.variables.getValue(varName)
            val allValues = variable.stream().map {
                check(it is Number) { "All variables to optimize must be Numbers. $varName has value $it." }
                it.toDouble()
            }.toList()
            allValues.min()..allValues.max()
        }
        val symplexVertices = (0..variables.size + 1).map {
            instances.mapValues { (_, range) -> randomGenerator.nextDouble(range.start, range.endInclusive) }
        }

        // now I need to run #seed times the simulations, with different seeds
        // the generated results will be used in the objective function to optimize

    }

    private fun goalFunction(objective: Double): (Collection<Double>) -> Double = { toOptimize ->
        (objective - geometricMean(toOptimize)).mod(2.0)
    }
}
