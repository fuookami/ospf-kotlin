package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

data class Cuboid3<V : FloatingNumber<V>>(
    val width: V,
    val height: V,
    val depth: V
) : Shape3<V> {
    override val boundingCuboid: Cuboid3<V> get() = this

    val volume: V get() = width * height * depth

    fun atOrigin(): Box3<V> = Box3.atOrigin(this)

    fun at(
        x: V,
        y: V,
        z: V
    ): Box3<V> = Box3(x = x, y = y, z = z, cuboid = this)

    fun along(axis: Axis3): V {
        return when (axis) {
            Axis3.X -> width
            Axis3.Y -> height
            Axis3.Z -> depth
        }
    }
}

