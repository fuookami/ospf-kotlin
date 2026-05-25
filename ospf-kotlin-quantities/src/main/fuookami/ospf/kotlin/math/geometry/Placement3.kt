package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

data class QuantityPlacement3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val shape: QuantityShape3<V>
) {
    val box: QuantityBox3<V>
        get() = QuantityBox3(
            x = x,
            y = y,
            z = z,
            cuboid = shape.boundingCuboid
        )

    val width: Quantity<V> get() = box.width
    val height: Quantity<V> get() = box.height
    val depth: Quantity<V> get() = box.depth
    val maxX: Quantity<V> get() = box.maxX
    val maxY: Quantity<V> get() = box.maxY
    val maxZ: Quantity<V> get() = box.maxZ

    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return box.contains(
            x = x,
            y = y,
            z = z,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    fun overlapped(rhs: QuantityPlacement3<V>): Boolean = box.overlapped(rhs.box)

    fun intersect(rhs: QuantityPlacement3<V>): QuantityPlacement3<V>? {
        val intersected = box.intersect(rhs.box) ?: return null
        return QuantityPlacement3(
            x = intersected.x,
            y = intersected.y,
            z = intersected.z,
            shape = intersected.cuboid
        )
    }
}

