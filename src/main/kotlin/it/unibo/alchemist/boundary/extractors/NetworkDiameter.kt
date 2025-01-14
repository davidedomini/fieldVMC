package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time

class NetworkDiameter() : Extractor<Int> {
    override val columnNames = listOf(NAME)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Int> {
        val diameter = findDiameter(environment)
        return mapOf(NAME to diameter)
    }

    private companion object {
        private const val NAME: String = "network-diameter"
    }

    fun <T> findDiameter(environment: Environment<T, *>): Int {
        fun bfs(start: Node<T>): Map<Node<T>, Int> {
            val distances = mutableMapOf<Node<T>, Int>()
            val queue: ArrayDeque<Node<T>> = ArrayDeque()

            distances[start] = 0
            queue.add(start)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val currentDistance = distances[current] ?: 0

                for (neighbor in environment.getNeighborhood(current)) {
                    if (neighbor !in distances) {
                        distances[neighbor] = currentDistance + 1
                        queue.add(neighbor)
                    }
                }
            }
            return distances
        }

        var diameter = 0
        for (node in environment.nodes) {
            val distances = bfs(node)
            val maxDistanceFromNode = distances.values.maxOrNull() ?: 0
            diameter = maxOf(diameter, maxDistanceFromNode)
        }

        return diameter
    }
}