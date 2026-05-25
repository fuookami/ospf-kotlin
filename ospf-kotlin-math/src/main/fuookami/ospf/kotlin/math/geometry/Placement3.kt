package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

data class Placement3<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val z: V,
    val shape: Shape3<V>
) {
    val box: Box3<V>
        get() = Box3(
            x = x,
            y = y,
            z = z,
            cuboid = shape.boundingCuboid
        )

    val width: V get() = box.width
    val height: V get() = box.height
    val depth: V get() = box.depth
    val maxX: V get() = box.maxX
    val maxY: V get() = box.maxY
    val maxZ: V get() = box.maxZ

    fun contains(
        x: V,
        y: V,
        z: V,
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

    fun overlapped(rhs: Placement3<V>): Boolean = box.overlapped(rhs.box)

    fun intersect(rhs: Placement3<V>): Placement3<V>? {
        val intersected = box.intersect(rhs.box) ?: return null
        return Placement3(
            x = intersected.x,
            y = intersected.y,
            z = intersected.z,
            shape = intersected.cuboid
        )
    }
}

