package it.unibo.alchemist.boundary.launchers

import it.unibo.alchemist.boundary.Launcher
import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.Variable
import it.unibo.alchemist.core.Simulation
import it.unibo.alchemist.model.Environment
import it.unibo.common.NelderMeadMethod
import org.danilopianini.rrmxmx.RrmxmxRandom
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NelderMeadLauncher
    @JvmOverloads
    constructor(
        private val objectiveFunction: Environment<*, *>.() -> Double,
        private val variables: List<String> = emptyList(),
        private val seedName: String,
        private val repetitions: Int = 1,
        private val autoStart: Boolean = false,
        private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
        private val maxIterations: Int = 1000,
        private val showProgress: Boolean = true,
        private val seed: ULong = RrmxmxRandom.DEFAULT_SEED,
        private val tolerance: Double = 1e-2,
        private val alpha: Double = 1.0, // standard value for the reflection in Nelder-Mead method
        private val gamma: Double = 2.0, // standard value for the expansion in Nelder-Mead method
        private val rho: Double = 0.5, // standard value for the contraction in Nelder-Mead method
        private val sigma: Double = 0.5, // standard value for the shrink in Nelder-Mead method
    ) : Launcher {
        @Synchronized
        override fun launch(loader: Loader) {
            require(loader.variables.isNotEmpty() || variables.isNotEmpty()) {
                "No variables found, can not optimize anything."
            }
            val simplexVertices: List<Map<String, Double>> = generateSymplexVertices(loader.variables)
            val seeds =
                loader.variables[seedName]
                    ?.stream()
                    ?.map {
                        check(it is Number) { "Seed must be a number. $it is not." }
                        it.toInt()
                    }?.toList()
                    ?.take(repetitions) ?: listOf(repetitions)
            when {
                parallelism == 1 -> {
                    loader.executeWithNelderMead(simplexVertices) { vertex ->
                        val result = mutableListOf<Double>()
                        for (s in seeds) {
                            // associate keys to vertex values
                            val simulationParameters = variables.associateWith { vertex[variables.indexOf(it)] } + (seedName to s)
                            val simulation = loader.getWith<Any, Nothing>(simulationParameters).configured()
                            simulation.run()
                            result.add(objectiveFunction.invoke(simulation.environment))
                        }
                        result.average()
                    }
                }
                else -> {
                    val launchId = launchId.getAndIncrement()
                    val workerId = AtomicInteger(0)
                    val executor =
                        Executors.newFixedThreadPool(parallelism) {
                            Thread(it, "Alchemist Pool $launchId worker ${workerId.getAndIncrement()}")
                        }
                    val errorQueue = ConcurrentLinkedDeque<Throwable>()
                    loader
                        .executeWithNelderMead(simplexVertices) { vertex ->
                            val result = mutableListOf<Double>()
                            for (s in seeds) {
                                // associate keys to vertex values
                                val simulationParameters = variables.associateWith { vertex[variables.indexOf(it)] } + ("seed" to s)
                                executor.submit {
                                    runCatching {
                                        loader.getWith<Any?, Nothing>(simulationParameters).configured().also {
                                            result.add(objectiveFunction.invoke(it.environment))
                                        }
                                    }.mapCatching { simulation ->
                                        simulation.run()
                                        simulation.error.ifPresent { throw it }
                                        logger.info("Simulation with {} completed successfully", simulationParameters)
                                    }.onFailure {
                                        logger.error("Failure for simulation with $simulationParameters", it)
                                        errorQueue.add(it)
                                        executor.shutdownNow()
                                    }.onSuccess {
                                        if (showProgress) {
                                            logger.info("Simulation with {} completed", simulationParameters)
                                        }




                                    }
                                }
                            }
                            result.average()
                        }.also { println("===== RISULTATISSIMO $it") }
                    executor.shutdown()
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
                    if (errorQueue.isNotEmpty()) {
                        throw errorQueue.reduce { previous, other ->
                            previous.addSuppressed(other)
                            previous
                        }
                    }
                }
            }
        }

        private fun Loader.executeWithNelderMead(
            simplexVertices: List<Map<String, Double>>,
            executeFunction: (DoubleArray) -> Double,
        ): Map<String, Double> =
            NelderMeadMethod(
                simplex = simplexVertices.map { it.values.toDoubleArray() }.toTypedArray(),
                objective = executeFunction,
                maxIterations = maxIterations,
                tolerance = tolerance,
                alpha = alpha,
                gamma = gamma,
                rho = rho,
                sigma = sigma,
            ).optimize()
                .let { result ->
                    this@NelderMeadLauncher.variables.associateWith { result[this@NelderMeadLauncher.variables.indexOf(it)] }
                }

        private fun Simulation<*, *>.configured() =
            apply {
                if (autoStart) {
                    play()
                }
            }

        private fun generateSymplexVertices(loaderVariables: Map<String, Variable<*>>): List<Map<String, Double>> {
            val randomGenerator = RrmxmxRandom(seed)
            val instances: Map<String, ClosedRange<Double>> =
                variables.associateWith { varName ->
                    val variable = loaderVariables.getValue(varName)
                    val allValues =
                        variable
                            .stream()
                            .map {
                                check(it is Number) { "All variables to optimize must be Numbers. $varName has value $it." }
                                it.toDouble()
                            }.toList()
                    allValues.min()..allValues.max()
                }
            return (0..variables.size).map {
                instances.mapValues { (_, range) -> randomGenerator.nextDouble(range.start, range.endInclusive) }
            }
        }

        protected companion object {
            /**
             * If no specific number of parallel threads to use is specified, this value is used.
             * Defaults to the number of logical cores detected by the JVM.
             */
            @JvmStatic
            protected val defaultParallelism = Runtime.getRuntime().availableProcessors()

            private val launchId = AtomicInteger(0)

            private val logger = LoggerFactory.getLogger(this::class.java)
        }
    }
