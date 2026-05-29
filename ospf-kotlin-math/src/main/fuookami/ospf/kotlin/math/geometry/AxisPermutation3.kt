/**
 * 三维轴置换
 * 3D Axis Permutation
 *
 * 定义三维几何空间中的轴置换，为纯几何概念，支持宽高深轴的交换与映射。
 * Defines axis permutation in 3D geometric space, a pure geometry concept supporting width/height/depth axis swapping and mapping.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维轴置换为纯几何概念；apply/mapAxis 可被桥接层复用。
 * 3D axis permutation is pure geometry; apply/mapAxis are reusable by bridges.
 *
 * @property widthAxis 宽度对应的轴 / The axis corresponding to width
 * @property heightAxis 高度对应的轴 / The axis corresponding to height
 * @property depthAxis 深度对应的轴 / The axis corresponding to depth
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
        /** 恒等置换 / Identity permutation */
        val XYZ = AxisPermutation3(Axis3.X, Axis3.Y, Axis3.Z)
        /** 完全反转 / Full reversal */
        val ZYX = AxisPermutation3(Axis3.Z, Axis3.Y, Axis3.X)
        /** Y-X-Z 置换 / Y-X-Z permutation */
        val YXZ = AxisPermutation3(Axis3.Y, Axis3.X, Axis3.Z)
        /** Z-X-Y 置换 / Z-X-Y permutation */
        val ZXY = AxisPermutation3(Axis3.Z, Axis3.X, Axis3.Y)
        /** X-Z-Y 置换 / X-Z-Y permutation */
        val XZY = AxisPermutation3(Axis3.X, Axis3.Z, Axis3.Y)
        /** Y-Z-X 置换 / Y-Z-X permutation */
        val YZX = AxisPermutation3(Axis3.Y, Axis3.Z, Axis3.X)
    }

    /**
     * 按轴置换成方体的宽、高、深
     * Permute the width, height, and depth of a cuboid by axes
     *
     * @param V 数值类型 / The numeric type
     * @param cuboid 待置换的长方体 / The cuboid to permute
     * @return 置换后的长方体 / The permuted cuboid
     */
    fun <V : FloatingNumber<V>> apply(cuboid: Cuboid3<V>): Cuboid3<V> {
        return Cuboid3(
            width = cuboid.along(widthAxis),
            height = cuboid.along(heightAxis),
            depth = cuboid.along(depthAxis)
        )
    }

    /**
     * 按轴置换圆柱的轴方向
     * Permute the axis direction of a cylinder
     *
     * @param V 数值类型 / The numeric type
     * @param cylinder 待置换的圆柱 / The cylinder to permute
     * @return 置换后的圆柱 / The permuted cylinder
     */
    fun <V : FloatingNumber<V>> apply(cylinder: Cylinder3<V>): Cylinder3<V> {
        return cylinder.copy(axis = mapAxis(cylinder.axis))
    }

    /**
     * 将原始轴映射到置换后的标准轴
     * Map an original axis to its permuted standard axis
     *
     * @param axis 原始轴 / The original axis
     * @return 置换后的标准轴 / The permuted standard axis
     */
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
