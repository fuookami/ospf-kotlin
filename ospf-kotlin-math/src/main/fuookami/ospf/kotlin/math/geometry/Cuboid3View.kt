/**
 * 三维长方体视图
 * 3D Cuboid View
 *
 * 定义三维长方体的轴置换视图，将原始长方体按轴置换后展示。
 * Defines axis-permuted view of a 3D cuboid, presenting the original cuboid after axis permutation.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维长方体的轴置换视图，将原始长方体按轴置换后展示。
 * Axis-permuted view of a 3D cuboid, presenting the original cuboid after axis permutation.
 *
 * @param V 数值类型 / The numeric type
 * @property origin 原始长方体 / The original cuboid
 * @property permutation 轴置换，默认为恒等置换 / Axis permutation, defaults to identity
 */
data class Cuboid3View<V : FloatingNumber<V>>(
    val origin: Cuboid3<V>,
    val permutation: AxisPermutation3 = AxisPermutation3.XYZ
) {
    /** 置换后的长方体 / The permuted cuboid */
    val cuboid: Cuboid3<V> get() = permutation.apply(origin)

    /** 置换后的宽度 / Permuted width */
    val width: V get() = cuboid.width
    /** 置换后的高度 / Permuted height */
    val height: V get() = cuboid.height
    /** 置换后的深度 / Permuted depth */
    val depth: V get() = cuboid.depth
}
