@file:Suppress("DEPRECATION")

package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.swingui.effect.api.Effect
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.nextUp

class DrawTreeNodes : Effect {
    override fun getColorSummary(): Color = Color.BLACK

    @Transient
    var lastUpdated = Time.NEGATIVE_INFINITY

    @Transient
    var maxSuccess = 0.0

    @Transient
    var maxResource = 0.0

    override fun <T : Any?, P : Position2D<P>> apply(
        g: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>,
    ) {
        if (environment.simulation.time != lastUpdated) {
            maxSuccess = 0.0
            maxResource = 0.0
            for (currentNode in environment.nodes) {
                maxSuccess = max(maxSuccess, currentNode.getConcentration(localSuccess).toDouble())
                maxResource = max(maxResource, currentNode.getConcentration(resource).toDouble())
            }
            lastUpdated = environment.simulation.time
        }
        runCatching {
            environment.getPosition(node)
        }.onSuccess { nodePosition ->
            val viewPoint = wormhole.getViewPoint(nodePosition)
            val localResource = node.getConcentration(resource).toDouble()
            val localSuccess = node.getConcentration(localSuccess).toDouble()
            val isLeader = node.getConcentration(leader) == true
            val size: Double = when {
                localResource > 0 && localSuccess > 0 -> localResource / maxResource + localSuccess / maxSuccess
                localResource > 0 -> localResource / maxResource
                localSuccess > 0 -> localSuccess / maxSuccess
                else -> 0.0.nextUp()
            }
            val sizeAsPosition: P = environment.makePosition(size, size)
            val sizeFromLocation = sizeAsPosition + nodePosition.coordinates
            val sizeInScreenCoordinates = (wormhole.getViewPoint(sizeFromLocation) - viewPoint)
                .let { Point(abs(it.x), abs(it.y)) }
                .takeIf { it.x > minNodeSize && it.y > minNodeSize }
                ?: Point(minNodeSize, minNodeSize)
            val boundingBoxSize = when {
                isLeader -> sizeInScreenCoordinates
                else -> sizeInScreenCoordinates / 2
            }
            val boundingBox = listOf(
                viewPoint + boundingBoxSize,
                viewPoint + boundingBoxSize.mirrorX(),
                viewPoint + boundingBoxSize.mirrorY(),
                viewPoint - boundingBoxSize,
            )
            if (isLeader) {
                g.stroke = BasicStroke(0.1f * sizeInScreenCoordinates.x.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.CAP_BUTT)
//                g.drawLine(viewPoint.x, viewPoint.y - boundingBoxSize.y, viewPoint.x, viewPoint.y + sizeInScreenCoordinates.y)
//                g.drawLine(viewPoint.x + boundingBoxSize.x, viewPoint.y, viewPoint.x - sizeInScreenCoordinates.x, viewPoint.y)
//                g.drawLine(viewPoint.x - boundingBoxSize.x, viewPoint.y - boundingBoxSize.y, viewPoint.x + sizeInScreenCoordinates.x, viewPoint.y + sizeInScreenCoordinates.y)
//                g.drawLine(viewPoint.x - boundingBoxSize.x, viewPoint.y + boundingBoxSize.y, viewPoint.x + sizeInScreenCoordinates.x, viewPoint.y - boundingBoxSize.y)
                listOf(
                    1 to 2,
                    6 to 9,
                ).forEach { (numerator, denominator) ->
                    g.color = Color(0x333333)
                    g.drawArc(
                        viewPoint.x - sizeInScreenCoordinates.x * numerator / denominator,
                        viewPoint.y - sizeInScreenCoordinates.y * numerator / denominator,
                        sizeInScreenCoordinates.x * 2 * numerator / denominator,
                        sizeInScreenCoordinates.y * 2 * numerator / denominator,
                        0,
                        360,
                    )
                }
            }
            if (boundingBox.any { wormhole.isInsideView(it) }) {
                g.color = Color.getHSBColor(
                    (localResource / maxResource).toFloat() * 0.8f,
                    1.0f,
                    (localSuccess / maxSuccess).toFloat() / 2 + 0.5f,
                )
                g.fillOval(
                    viewPoint.x - sizeInScreenCoordinates.x / 2,
                    viewPoint.y - sizeInScreenCoordinates.y / 2,
                    sizeInScreenCoordinates.x,
                    sizeInScreenCoordinates.y,
                )
                g.color = Color.BLACK
                // Arrow stroke
                g.stroke = BasicStroke(size.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.CAP_BUTT)
                g.drawArc(
                    viewPoint.x - sizeInScreenCoordinates.x / 2,
                    viewPoint.y - sizeInScreenCoordinates.y / 2,
                    sizeInScreenCoordinates.x,
                    sizeInScreenCoordinates.y,
                    0,
                    360,
                )
            }
        }
    }

    companion object {
        val myParent = SimpleMolecule("myParent")
        val localSuccess = SimpleMolecule("success")
        val resource = SimpleMolecule("resource")
        val leader = SimpleMolecule("leader")
        val minNodeSize = 10

        private fun Any?.toInt(): Int? = when (this) {
            is Int -> this
            is Number -> this.toInt()
            is String -> this.toInt()
            null -> null
            Unit -> null
            else -> error("Unexpected integer: $this")
        }

        private fun Any?.toDouble(): Double = when (this) {
            is Double -> this
            is Number -> this.toDouble()
            null -> 0.0
            Unit -> 0.0
            else -> error("Unexpected integer: $this")
        }

        private operator fun Point.plus(other: Point): Point = Point(x + other.x, y + other.y)
        private operator fun Point.minus(other: Point): Point = Point(x - other.x, y - other.y)
        private operator fun Point.times(factor: Int): Point = Point((x * factor), (y * factor))
        private operator fun Point.div(factor: Int): Point = Point((x / factor), (y / factor))
        private fun Point.mirrorX(): Point = Point(-x, y)
        private fun Point.mirrorY(): Point = Point(x, -y)
    }
}
