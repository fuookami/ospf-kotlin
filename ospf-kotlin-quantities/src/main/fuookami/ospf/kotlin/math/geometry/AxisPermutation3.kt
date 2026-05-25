package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维轴置换为纯几何概念；apply/mapAxis 可被桥接层复用。
 * 3D axis permutation is pure geometry; apply/mapAxis are reusable by bridges.
 */
data class QuantityAxisPermutation3(
    val widthAxis: Axis3,
    val heightAxis: Axis3,
    val depthAxis: Axis3
) {
    init {
        require(setOf(widthAxis, heightAxis, depthAxis).size == 3) {
            "QuantityAxisPermutation3 requires three distinct axes."
        }
    }

    companion object {
        val XYZ = QuantityAxisPermutation3(Axis3.X, Axis3.Y, Axis3.Z)
        val ZYX = QuantityAxisPermutation3(Axis3.Z, Axis3.Y, Axis3.X)
        val YXZ = QuantityAxisPermutation3(Axis3.Y, Axis3.X, Axis3.Z)
        val ZXY = QuantityAxisPermutation3(Axis3.Z, Axis3.X, Axis3.Y)
        val XZY = QuantityAxisPermutation3(Axis3.X, Axis3.Z, Axis3.Y)
        val YZX = QuantityAxisPermutation3(Axis3.Y, Axis3.Z, Axis3.X)
    }

    fun <V : FloatingNumber<V>> apply(cuboid: QuantityCuboid3<V>): QuantityCuboid3<V> {
        return QuantityCuboid3(
            width = cuboid.along(widthAxis),
            height = cuboid.along(heightAxis),
            depth = cuboid.along(depthAxis)
        )
    }

    fun <V : FloatingNumber<V>> apply(cylinder: QuantityCylinder3<V>): QuantityCylinder3<V> {
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

