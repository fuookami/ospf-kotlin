package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

data class QuantityPlacement2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val shape: QuantityProjection2<V>
) {
    private val box: QuantityBox2<V> get() = QuantityBox2(x = x, y = y, shape = shape)

    val width: Quantity<V>
        get() = box.width

    val height: Quantity<V>
        get() = box.height

    val maxX: Quantity<V> get() = box.maxX
    val maxY: Quantity<V> get() = box.maxY

    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return box.contains(
            x = x,
            y = y,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    fun overlapped(rhs: QuantityPlacement2<V>): Boolean {
        return box.overlapped(rhs.box)
    }

    fun intersect(rhs: QuantityPlacement2<V>): QuantityPlacement2<V>? {
        val intersection = box.intersect(rhs.box) ?: return null
        return QuantityPlacement2(
            x = intersection.x,
            y = intersection.y,
            shape = intersection.shape
        )
    }
}

