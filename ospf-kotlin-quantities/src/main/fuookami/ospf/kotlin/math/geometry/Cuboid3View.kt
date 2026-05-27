/**
 * 三维长方体视图
 * 3D cuboid view
 *
 * 通过轴置换对原始长方体进行视角变换，提供变换后的尺寸。
 * Transforms an original cuboid through axis permutation, providing transformed dimensions.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 三维长方体视图
 * 3D cuboid view
 *
 * 通过轴置换对原始长方体进行视角变换，提供变换后的尺寸。
 * Transforms an original cuboid through axis permutation, providing transformed dimensions.
 *
 * @property origin 原始长方体 / Original cuboid
 * @property permutation 轴置换（默认 XYZ）/ Axis permutation (default XYZ)
 * @param V 数值类型 / Number type
 */
data class QuantityCuboid3View<V : FloatingNumber<V>>(
    val origin: QuantityCuboid3<V>,
    val permutation: QuantityAxisPermutation3 = QuantityAxisPermutation3.XYZ
) {
    /** 置换后的长方体 / Permuted cuboid */
    val cuboid: QuantityCuboid3<V> get() = permutation.apply(origin)

    /** 视图宽度 / View width */
    val width: Quantity<V> get() = cuboid.width
    /** 视图高度 / View height */
    val height: Quantity<V> get() = cuboid.height
    /** 视图深度 / View depth */
    val depth: Quantity<V> get() = cuboid.depth
}
