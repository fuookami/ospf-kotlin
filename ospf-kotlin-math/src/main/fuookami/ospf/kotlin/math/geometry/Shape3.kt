/**
 * 三维形状接口
 * 3D Shape Interface
 *
 * 定义三维形状的通用接口，提供包围长方体的访问。
 * Defines common interface for 3D shapes, providing access to the bounding cuboid.
*/
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 三维形状的通用接口，提供包围长方体的访问。
 * Common interface for 3D shapes, providing access to the bounding cuboid.
 *
 * @param V 数值类型 / The numeric type
*/
interface Shape3<V : FloatingNumber<V>> {

    /** 包围长方体 / The bounding cuboid */
    val boundingCuboid: Cuboid3<V>
}
