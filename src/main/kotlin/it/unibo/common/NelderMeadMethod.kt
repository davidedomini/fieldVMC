package it.unibo.common

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
class NelderMeadMethod(
    val simplex: Array<DoubleArray>,
    private val objective: (DoubleArray) -> Double,
    private val maxIterations: Int,
    private val tolerance: Double,
    private val alpha: Double, // Reflection coefficient
    private val gamma: Double, // Expansion coefficient
    private val rho: Double, // Contraction coefficient
    private val sigma: Double, // Shrink coefficient
) {
    /**
     * Apply the Nelder-Mead optimization method to the given [simplex] and [objective] function.
     */
    fun optimize(): DoubleArray {
        val numPoints = simplex.size
        val numDims = simplex[0].size
        require(numPoints == numDims + 1) {
            "The vertices of the initial simplex must be one more than the number of dimensions"
        }
        repeat(maxIterations) {
            // Sort simplex by function values
            simplex.sortBy { objective(it) }
            val best = simplex[0]
            val worst = simplex[numPoints - 1]
            val secondWorst = simplex[numPoints - 2]
            // Compute centroid (excluding worst point)
            val centroid =
                DoubleArray(numDims) { j ->
                    simplex.take(numPoints - 1).sumOf { it[j] } / (numPoints - 1)
                }
            // Reflection
            val reflected = centroid.mapIndexed { j, v -> v + alpha * (v - worst[j]) }.toDoubleArray()
            val reflectedValue = objective(reflected)

            if (reflectedValue < objective(best)) {
                // Expansion
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
                val contracted =
                    if (reflectedValue < objective(worst)) {
                        centroid.mapIndexed { j, v -> v + rho * (reflected[j] - v) }.toDoubleArray()
                    } else {
                        centroid.mapIndexed { j, v -> v + rho * (worst[j] - v) }.toDoubleArray()
                    }
                if (objective(contracted) < objective(worst)) {
                    simplex[numPoints - 1] = contracted
                } else {
                    // Shrink simplex
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
