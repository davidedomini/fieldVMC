package it.unibo.collektive.alchemist.device.sensors

interface DeviceSpawn {
    /**
     * Spawns a new node in the given [coordinate].
     */
    fun spawn(coordinate: Pair<Double, Double>): Double

    /**
     * The node will destroy itself.
     */
    fun selfDestroy()

    /**
     * Returns the current time inside the simulation environment.
     */
    fun currentTime(): Double

    val cloningRange: Double

    val maxChildren: Int

    val minSpawnWait: Double
}
