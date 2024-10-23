package it.unibo.collektive.alchemist.device.sensors.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.alchemist.device.sensors.LeaderSensor

class LeaderSensorProperty<T, P : Position<P>>(
    override val leaderRadius: Double,
    private val environment: Environment<T, P>,
    override val node: Node<T>,
) : LeaderSensor, NodeProperty<T> {
    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        LeaderSensorProperty(leaderRadius, environment, node)

    override fun isLeader(): Boolean {
        val leaderMolecule = SimpleMolecule("leader")
        return if (node.contains(leaderMolecule)) node.getConcentration(leaderMolecule) as Boolean else false
    }

    @Suppress("UNCHECKED_CAST")
    override fun setLeader(leader: Boolean) {
        node.setConcentration(SimpleMolecule("leader"), leader as T)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <ID : Any> setLeaderId(id: ID) =
        node.setConcentration(SimpleMolecule("leaderId"), id as T)
}
