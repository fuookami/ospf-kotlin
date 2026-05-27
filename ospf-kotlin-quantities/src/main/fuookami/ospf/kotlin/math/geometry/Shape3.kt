/**
 * 三维形状接口
 * 3D shape interface
 *
 * 定义所有三维形状的公共契约，提供最小包围长方体。
 * Defines the common contract for all 3D shapes, providing the minimum bounding cuboid.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维形状接口
 * 3D shape interface
 *
 * 所有三维形状的公共契约，提供最小包围长方体。
 * Common contract for all 3D shapes, providing the minimum bounding cuboid.
 *
 * @property boundingCuboid 最小包围长方体 / Minimum bounding cuboid
 */
interface QuantityShape3<V : FloatingNumber<V>> {
    val boundingCuboid: QuantityCuboid3<V>
}
