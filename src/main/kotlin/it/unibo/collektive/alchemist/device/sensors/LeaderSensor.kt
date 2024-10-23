package it.unibo.collektive.alchemist.device.sensors

interface LeaderSensor {
    val leaderRadius: Double

    /**
     * Checks if the node is considered leader.
     */
    fun isLeader(): Boolean

    /**
     * Set the node as leader.
     */
    fun setLeader(leader: Boolean)

    /**
     * Set the node's leader [id].
     */
    fun <ID : Any> setLeaderId(id: ID)
}
