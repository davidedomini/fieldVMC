package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.ExportFilter
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.util.StatUtil
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import kotlin.math.min

// aggregators max, mean, stdev
class NetworkDegree
    @JvmOverloads
    constructor(
        private val property: String?,
        private val filter: ExportFilter,
        aggregatorNames: List<String>,
        precision: Int? = null,
) : AbstractDoubleExporter(precision) {

    private companion object {
        private const val NAME = "network-grade"
        private const val SHORT_NAME_MAX_LENGTH = 5
    }

    private val aggregators: Map<String, UnivariateStatistic> =
        aggregatorNames
            .associateWith { StatUtil.makeUnivariateStatistic(it) }
            .filter { it.value.isPresent }
            .map { it.key to it.value.get() }
            .toMap()

    private val propertyText =
        if (property.isNullOrEmpty()) {
            ""
        } else {
            property.replace("[^\\d\\w]*".toRegex(), "")
        }

    private val shortProp =
        propertyText.takeIf(String::isEmpty)
            ?: "${propertyText.substring(0..<min(propertyText.length, SHORT_NAME_MAX_LENGTH))}@"

    private val singleColumnName: String = "$shortProp$NAME"

    override val columnNames: List<String> =
        aggregators.keys
            .takeIf { it.isNotEmpty() }
            ?.map { "$singleColumnName[$it]" }
            ?: listOf("$singleColumnName@node-id")


    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Double> {
        val filtered = environment.nodes
            .map { n -> environment.getNeighborhood(n).size() }
            .flatMap { t -> filter.apply(t.toDouble()) }
            .toDoubleArray()
        return aggregators.map { (aggregatorName, aggregator) ->
            "$singleColumnName[$aggregatorName]" to aggregator.evaluate(filtered)
        }.toMap()
    }
}