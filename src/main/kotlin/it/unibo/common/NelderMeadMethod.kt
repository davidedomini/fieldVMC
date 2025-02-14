package it.unibo.common

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Nelder-Mead optimization method.
 * Given an initial [simplex], this method iteratively refines the simplex to minimize a given [objective] function.
 * The method is suitable for optimizing functions that are continuous but not differentiable.
 * Other parameters are:
 * - [alpha]: Reflection coefficient (standard value is 1.0);
 * - [gamma]: Expansion coefficient (standard value is 2.0);
 * - [rho]: Contraction coefficient (standard value is 0.5);
 * - [sigma]: Shrink coefficient (standard value is 0.5);
 * - [maxIterations]: Maximum number of iterations;
 * - [tolerance]: Termination condition (small variation in function values).
 */
@JvmInline
value class Vertex(private val vertex: Map<String, Double>) {
    val size: Int
        get() = vertex.size

    fun valuesToList(): List<Double> = vertex.values.toList()

    operator fun get(index: Int) = valuesToList()[index]
}

class NelderMeadMethod(
    val simplex: List<Vertex>,
    private val maxIterations: Int,
    private val tolerance: Double,
    private val alpha: Double, // Reflection coefficient
    private val gamma: Double, // Expansion coefficient
    private val rho: Double, // Contraction coefficient
    private val sigma: Double, // Shrink coefficient
    executorService: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
    private val objective: (List<Double>) -> Future<Double>
) {

    private val cache = Caffeine.newBuilder().executor(executorService)
        .build<List<Double>, Future<Double>> { coordinates ->
            objective(coordinates)
    }

    /**
     * Apply the Nelder-Mead optimization method to the given [simplex] and [objective] function.
     */
    fun optimize(): List<Double> {
        require(simplex.isNotEmpty()) { "The initial simplex must not be empty" }
        val dimensions = simplex.first().size
        require(dimensions > 0) { "The number of dimensions must be greater than 0" }
        require(simplex.size == dimensions + 1) {
            "The vertices of the initial simplex must be one more than the number of dimensions"
        }
        require(simplex.all { it.size == dimensions }) {
            "All vertices of the initial simplex must have the same number of dimensions"
        }
        repeat(maxIterations) {
            // Sort simplex by function values
            val sortedSimplex = simplex
                .map { it to cache[it.valuesToList()] }
                .sortedBy { it.second.get() }
                .map { it.first }
            val bestVertex: Vertex = sortedSimplex.first()
            val worstVertex: Vertex = simplex.last()
            val secondWorstVertex: Vertex = simplex[simplex.size - 2]
            val bestValue = cache[bestVertex.valuesToList()]
            val worstValues = worstVertex.valuesToList()
            // Compute centroid (excluding worst point)
            val centroid =
                DoubleArray(dimensions) { index ->
                    simplex.take(simplex.size - 1).sumOf { it.valuesToList()[index] } / (simplex.size - 1)
                }.toList()
            // Reflections
            val reflected: List<Double> = centroid.mapCentroid(alpha, worstValues)
            val reflectedValue = cache[reflected]
            val newSymplex: List<Vertex> = if (reflectedValue < bestValue) {
                // Expansion
                val expanded: List<Double> = centroid.mapCentroid(gamma, reflected)
                if (cache[expanded] < reflectedValue) {
                    simplex.updateLast(expanded)
                } else {
                    simplex.updateLast(reflected)
                }
            } else if (reflectedValue < cache[secondWorstVertex.valuesToList()]) {
                // Accept reflection
                simplex.updateLast(reflected)
            } else {
                // Contraction
                val contracted =
                    if (reflectedValue < cache[worstValues]) {
                        centroid.mapCentroid(rho, reflected)
                    } else {
                        centroid.mapCentroid(rho, worstValues)
                    }
                if (objective(contracted) < cache[worstValues]) {
                    simplex.updateLast(contracted)
                } else {
                    // Shrink simplex
                    for (i in 1 until simplex.size) {
                        simplex.mapIndexed { index, vertex ->
                            if (i == index) {
                                vertex.valuesToList().mapIndexed { at, value -> bestVertex[at] + sigma * (value - bestVertex[at]) }
                            } else
                                vertex
                        }
                    }
                }
            }
            // Check termination condition (small variation in function values)
            val functionValues = simplex.map { cache[it.valuesToList()].get() }
            val maxDiff = functionValues.maxOrNull()!! - functionValues.minOrNull()!!
            if (maxDiff < tolerance) return simplex.first().valuesToList()
        }
        return simplex.first().valuesToList()
    }

    private fun List<Double>.mapCentroid(coefficient: Double, values: List<Double>): List<Double> =
        mapIndexed { index, value -> value + coefficient * (values[index] - value) }

    private fun List<Vertex>.updateLast(newVertex: List<Double>): List<Vertex> =
        mapIndexed { i, value -> if(i == simplex.size - 1) newVertex else value}

    companion object {
        operator fun Future<Double>.compareTo(other: Future<Double>): Int = get().compareTo(other.get())
        operator fun Future<Double>.compareTo(other: Double): Int = get().compareTo(other)
        operator fun Double.compareTo(other: Future<Double>): Int = compareTo(other.get())
    }
}



