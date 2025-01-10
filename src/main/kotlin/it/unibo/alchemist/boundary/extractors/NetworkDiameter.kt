package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time

class NetworkDiameter() : Extractor<Int> {
    override val columnNames = listOf(NAME)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Int> {
        TODO("Not yet implemented")
    }

    private companion object {
        private const val NAME: String = "network-diameter"
    }
}