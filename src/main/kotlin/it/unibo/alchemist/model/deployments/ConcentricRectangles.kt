package it.unibo.alchemist.model.deployments

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.util.RandomGenerators.nextDouble
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.absoluteValue

class ConcentricRectangles<P: Position<P>>(
    environment: Environment<*, P>,
    randomGenerator: RandomGenerator,
    val nodes: Int,
    outerStartX: Double,
    outerStartY: Double,
    outerWidth: Double,
    outerHeight: Double,
    innerStartX: Double,
    innerStartY: Double,
    innerWidth: Double,
    innerHeight: Double,
) : AbstractRandomDeployment<P>(environment, randomGenerator, nodes) {
    data class Rectangle(val startX: Double, val startY: Double, val width: Double, val height: Double){
        val area = width * height
        override fun toString(): String = "Rectangle(start($startX, $startY), finish(${startX + width}, ${startY + height})), " +
                "width: $width, height: $height, area: $area"
    }

    companion object{
        lateinit var rectangles : List<Rectangle>
    }

    init {
        rectangles = listOf<Rectangle>(
            Rectangle(outerStartX, outerStartY, (innerStartX - outerStartX).absoluteValue, outerHeight),
            Rectangle(innerStartX, outerStartY, innerWidth, (innerStartY - outerStartY).absoluteValue),
            Rectangle(innerStartX, innerStartY + innerHeight, innerWidth, ((outerStartY + outerHeight) - (innerStartY + innerHeight)).absoluteValue),
            Rectangle(innerStartX + innerWidth, outerStartY, ((outerStartX + outerWidth) - (innerStartX + innerWidth)).absoluteValue, outerHeight),
        )
    }

    override fun indexToPosition(i: Int): P {
        val randomArea = randomGenerator.nextDouble(0.0, rectangles.sumOf { it.area })
        var currentArea = 0.0
        return rectangles.first {
            currentArea += it.area
            randomArea <= currentArea
        }.let { rectangle ->
            environment.makePosition(
                randomGenerator.nextDouble(rectangle.startX, rectangle.startX + rectangle.width),
                randomGenerator.nextDouble(rectangle.startY, rectangle.startY + rectangle.height)
            )
        }
    }
}