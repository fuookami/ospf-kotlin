package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

data class Placement2<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val shape: Projection2<V>
) {
    private val box: Box2<V> get() = Box2(x = x, y = y, shape = shape)

    val width: V
        get() = box.width

    val height: V
        get() = box.height

    val maxX: V get() = box.maxX
    val maxY: V get() = box.maxY

    fun contains(
        x: V,
        y: V,
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

    fun overlapped(rhs: Placement2<V>): Boolean {
        return box.overlapped(rhs.box)
    }

    fun intersect(rhs: Placement2<V>): Placement2<V>? {
        val intersection = box.intersect(rhs.box) ?: return null
        return Placement2(
            x = intersection.x,
            y = intersection.y,
            shape = intersection.shape
        )
    }
}

