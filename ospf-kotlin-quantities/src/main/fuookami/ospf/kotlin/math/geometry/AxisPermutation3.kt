/**
 * 三维轴置换
 * 3D axis permutation
 *
 * 定义三维坐标轴的置换操作，支持对长方体和包围盒进行轴重排。
 * Defines 3D axis permutation operations, supporting axis rearrangement for cuboids and bounding boxes.
*/
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 三维轴置换为纯几何概念；apply/mapAxis 可被桥接层复用。
 * 3D axis permutation is pure geometry; apply/mapAxis are reusable by bridges.
 *
 * @property widthAxis 宽度对应的轴 / Axis corresponding to width
 * @property heightAxis 高度对应的轴 / Axis corresponding to height
 * @property depthAxis 深度对应的轴 / Axis corresponding to depth
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
        /** X-Y-Z 置换 / X-Y-Z permutation */
        val XYZ = QuantityAxisPermutation3(Axis3.X, Axis3.Y, Axis3.Z)

        /** Z-Y-X 置换 / Z-Y-X permutation */
        val ZYX = QuantityAxisPermutation3(Axis3.Z, Axis3.Y, Axis3.X)

        /** Y-X-Z 置换 / Y-X-Z permutation */
        val YXZ = QuantityAxisPermutation3(Axis3.Y, Axis3.X, Axis3.Z)

        /** Z-X-Y 置换 / Z-X-Y permutation */
        val ZXY = QuantityAxisPermutation3(Axis3.Z, Axis3.X, Axis3.Y)

        /** X-Z-Y 置换 / X-Z-Y permutation */
        val XZY = QuantityAxisPermutation3(Axis3.X, Axis3.Z, Axis3.Y)

        /** Y-Z-X 置换 / Y-Z-X permutation */
        val YZX = QuantityAxisPermutation3(Axis3.Y, Axis3.Z, Axis3.X)
    }

    /**
     * 对长方体应用轴置换
     * Apply axis permutation to a cuboid
     *
     * @param cuboid 待置换的长方体 / Cuboid to permute
     * @param V 数值类型 / Number type
     * @return 置换后的长方体 / Permuted cuboid
    */
    fun <V : FloatingNumber<V>> apply(cuboid: QuantityCuboid3<V>): QuantityCuboid3<V> {
        return QuantityCuboid3(
            width = cuboid.along(widthAxis),
            height = cuboid.along(heightAxis),
            depth = cuboid.along(depthAxis)
        )
    }

    /**
     * 对圆柱体应用轴置换
     * Apply axis permutation to a cylinder
     *
     * @param cylinder 待置换的圆柱体 / Cylinder to permute
     * @param V 数值类型 / Number type
     * @return 置换后的圆柱体结果 / Permuted cylinder result
    */
    fun <V : FloatingNumber<V>> apply(cylinder: QuantityCylinder3<V>): Ret<QuantityCylinder3<V>> {
        return mapAxis(cylinder.axis).map { axis ->
            cylinder.copy(axis = axis)
        }
    }

    /**
     * 将原始轴映射到置换后的轴
     * Map an original axis to its permuted counterpart
     *
     * @param axis 原始轴 / Original axis
     * @return 置换后的轴结果 / Permuted axis result
    */
    fun mapAxis(axis: Axis3): Ret<Axis3> {
        return if (axis == widthAxis) {
            Ok(Axis3.X)
        } else if (axis == heightAxis) {
            Ok(Axis3.Y)
        } else if (axis == depthAxis) {
            Ok(Axis3.Z)
        } else {
            Failed(ErrorCode.IllegalArgument, "Unsupported axis: $axis")
        }
    }
}
