package it.unibo.collektive.alchemist.device.sensors

interface ResourceSensor {
    val resourceLowerBound: Double

    /**
     * Get the value of the resource layer at a given position.
     */
    fun getResource(): Double

    /**
     * Set a new [resource] value for this node.
     */
    fun setCurrentOverallResource(resource: Double)
}
