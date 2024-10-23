package it.unibo.collektive.alchemist.device.sensors.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.alchemist.device.sensors.ResourceSensor

class ResourceSensorProperty<T, P : Position<P>>(
    private val environment: Environment<T, P>,
    override val node: Node<T>,
    override val resourceLowerBound: Double,
) : ResourceSensor, NodeProperty<T> {

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        ResourceSensorProperty(environment, node, resourceLowerBound)

    override fun getResource(): Double =
        when (val layerValue = environment.getLayer(localResource).get().getValue(environment.getPosition(node))) {
            is Number -> layerValue.toDouble()
            else -> error("ResourceSensorProperty: $layerValue is not a number")
        }

    @Suppress("UNCHECKED_CAST")
    override fun setCurrentOverallResource(resource: Double) =
        node.setConcentration(resourceMolecule, resource as T)

    companion object {
        private val resourceMolecule = SimpleMolecule("resource")
        private val localResource = SimpleMolecule("localResource")
    }
}
