package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times

data class QuantityCuboid3<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>
) : QuantityShape3<V> {
    override val boundingCuboid: QuantityCuboid3<V> get() = this

    val volume: Quantity<V> get() = width * height * depth

    fun atOrigin(): QuantityBox3<V> = QuantityBox3.atOrigin(this)

    fun at(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>
    ): QuantityBox3<V> = QuantityBox3(x = x, y = y, z = z, cuboid = this)

    fun along(axis: Axis3): Quantity<V> {
        return when (axis) {
            Axis3.X -> width
            Axis3.Y -> height
            Axis3.Z -> depth
        }
    }
}

