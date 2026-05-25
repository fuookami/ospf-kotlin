package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维轴置换为纯几何概念；apply/mapAxis 可被桥接层复用。
 * 3D axis permutation is pure geometry; apply/mapAxis are reusable by bridges.
 */
data class AxisPermutation3(
    val widthAxis: Axis3,
    val heightAxis: Axis3,
    val depthAxis: Axis3
) {
    init {
        require(setOf(widthAxis, heightAxis, depthAxis).size == 3) {
            "AxisPermutation3 requires three distinct axes."
        }
    }

    companion object {
        val XYZ = AxisPermutation3(Axis3.X, Axis3.Y, Axis3.Z)
        val ZYX = AxisPermutation3(Axis3.Z, Axis3.Y, Axis3.X)
        val YXZ = AxisPermutation3(Axis3.Y, Axis3.X, Axis3.Z)
        val ZXY = AxisPermutation3(Axis3.Z, Axis3.X, Axis3.Y)
        val XZY = AxisPermutation3(Axis3.X, Axis3.Z, Axis3.Y)
        val YZX = AxisPermutation3(Axis3.Y, Axis3.Z, Axis3.X)
    }

    fun <V : FloatingNumber<V>> apply(cuboid: Cuboid3<V>): Cuboid3<V> {
        return Cuboid3(
            width = cuboid.along(widthAxis),
            height = cuboid.along(heightAxis),
            depth = cuboid.along(depthAxis)
        )
    }

    fun <V : FloatingNumber<V>> apply(cylinder: Cylinder3<V>): Cylinder3<V> {
        return cylinder.copy(axis = mapAxis(cylinder.axis))
    }

    fun mapAxis(axis: Axis3): Axis3 {
        return if (axis == widthAxis) {
            Axis3.X
        } else if (axis == heightAxis) {
            Axis3.Y
        } else if (axis == depthAxis) {
            Axis3.Z
        } else {
            throw IllegalArgumentException("Unsupported axis: $axis")
        }
    }
}

