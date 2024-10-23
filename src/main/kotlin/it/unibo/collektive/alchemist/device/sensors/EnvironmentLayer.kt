package it.unibo.collektive.alchemist.device.sensors

/**
 * A layer of the environment that can be accessed by the devices.
 */
interface EnvironmentLayer {
    /**
     * Get the value at a given position of the [name] layer.
     */
    fun <T> getFromLayer(name: String): T

    /**
     * Get the value at a given position of the [name] layer or returns null.
     */
    fun <T> getFromLayerOrNull(name: String): T?

    /**
     * Get the value at a given position of the [name] layer or returns the [default].
     */
    fun <T> getFromLayerOrDefault(name: String, default: T): T

    /**
     * Checks if the layer exists inside the simulation environment.
     */
    fun isLayerDefined(name: String): Boolean
}
