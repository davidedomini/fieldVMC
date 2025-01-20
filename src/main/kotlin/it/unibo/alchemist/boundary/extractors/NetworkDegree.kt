package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.ExportFilter
import it.unibo.alchemist.boundary.exportfilters.CommonFilters
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.util.StatUtil
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import kotlin.math.sqrt

class NetworkDegree
    @JvmOverloads
    constructor(
        private val filter: ExportFilter = CommonFilters.ONLYFINITE.filteringPolicy,
        aggregatorNames: List<String> = listOf("max", "mean", "stdev"),
        precision: Int = 2,
    ) : AbstractDoubleExporter(precision) {
        private companion object {
            private const val NAME = "network-degree"
        }

        private val aggregators: Map<String, UnivariateStatistic> =
            aggregatorNames
                .associateWith { StatUtil.makeUnivariateStatistic(it) }
                .filter { it.value.isPresent }
                .map { it.key to it.value.get() }
                .toMap()

        override val columnNames: List<String> =
            aggregators.keys
                .takeIf { it.isNotEmpty() }
                ?.map { "$NAME[$it]" }
                ?: listOf("$NAME@node-id")

        fun <T> calculateDegreeStatistics(environment: Environment<T, *>): Map<String, Double> {
            val degrees = environment.nodes.map { environment.getNeighborhood(it).size() }

            val maxDegree = degrees.maxOrNull()?.toDouble() ?: 0.0
            val averageDegree = degrees.average()
            val variance =
                degrees.fold(0.0) { acc, degree ->
                    acc + (degree - averageDegree) * (degree - averageDegree)
                } / degrees.size
            val stdDeviation = sqrt(variance)

            return mapOf(
                "$NAME.max" to maxDegree,
                "$NAME.average" to averageDegree,
                "$NAME.std_deviation" to stdDeviation,
            )
        }

        override fun <T> extractData(
            environment: Environment<T, *>,
            reaction: Actionable<T>?,
            time: Time,
            step: Long,
        ): Map<String, Double> = calculateDegreeStatistics(environment)
//        val stats = calculateDegreeStatistics(environment)
//        val filtered = environment.nodes
//            .map { n -> environment.getNeighborhood(n).size() }
//            .flatMap { t -> filter.apply(t.toDouble()) }
//            .toDoubleArray()
//        return aggregators.map { (aggregatorName, aggregator) ->
//            "$singleColumnName[$aggregatorName]" to aggregator.evaluate(filtered)
//        }.toMap()
//    }
    }
