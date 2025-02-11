package it.unibo.common

import kotlin.math.pow

class NelderMeadAlgorithm(
    private val objective: (DoubleArray) -> Double,
    private val maxIterations: Int = 1000,
    private val tolerance: Double = 1e-2
) {
    fun optimize(simplex: Array<DoubleArray>): DoubleArray {
        val numPoints = simplex.size
        val numDims = simplex[0].size
        require(numPoints == numDims + 1) {
            "Simplex must have one more vertex than the number of dimensions"
        }
        repeat(maxIterations) {
            // Sort simplex by function values
            simplex.sortBy { objective(it) }
            val best = simplex[0]
            val worst = simplex[numPoints - 1]
            val secondWorst = simplex[numPoints - 2]

            // Compute centroid (excluding worst point)
            val centroid = DoubleArray(numDims) { j ->
                simplex.take(numPoints - 1).sumOf { it[j] } / (numPoints - 1)
            }

            // Reflection
            val alpha = 1.0
            val reflected = centroid.mapIndexed { j, v -> v + alpha * (v - worst[j]) }.toDoubleArray()
            val reflectedValue = objective(reflected)

            if (reflectedValue < objective(best)) {
                // Expansion
                val gamma = 2.0
                val expanded = centroid.mapIndexed { j, v -> v + gamma * (reflected[j] - v) }.toDoubleArray()
                if (objective(expanded) < reflectedValue) {
                    simplex[numPoints - 1] = expanded
                } else {
                    simplex[numPoints - 1] = reflected
                }
            } else if (reflectedValue < objective(secondWorst)) {
                // Accept reflection
                simplex[numPoints - 1] = reflected
            } else {
                // Contraction
                val rho = 0.5
                val contracted = if (reflectedValue < objective(worst)) {
                    centroid.mapIndexed { j, v -> v + rho * (reflected[j] - v) }.toDoubleArray()
                } else {
                    centroid.mapIndexed { j, v -> v + rho * (worst[j] - v) }.toDoubleArray()
                }
                if (objective(contracted) < objective(worst)) {
                    simplex[numPoints - 1] = contracted
                } else {
                    // Shrink simplex
                    val sigma = 0.5
                    for (i in 1 until numPoints) {
                        simplex[i] = simplex[i].mapIndexed { j, v -> best[j] + sigma * (v - best[j]) }.toDoubleArray()
                    }
                }
            }

            // Check termination condition (small variation in function values)
            val functionValues = simplex.map { objective(it) }
            val maxDiff = functionValues.maxOrNull()!! - functionValues.minOrNull()!!
            if (maxDiff < tolerance) return simplex[0]
        }
        return simplex[0]
    }
}

fun main() {
    val objectiveFunction: (DoubleArray) -> Double = { x -> (x[0] - 3).pow(2) + (x[1] - 3).pow(3) }
    val initialSimplex = arrayOf(
        doubleArrayOf(0.0, 0.0),
        doubleArrayOf(10.0, 10.0),
        doubleArrayOf(2.0, 0.0)
    )

    val optimizer = NelderMeadAlgorithm(objectiveFunction)
    val result = optimizer.optimize(initialSimplex)
    println("Optimal solution: x = ${result.joinToString()}")
    println("Objective value: ${objectiveFunction(result)}")
    println("Ideal ${objectiveFunction(doubleArrayOf(3.0, 3.0))}")
}
