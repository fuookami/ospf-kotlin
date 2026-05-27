/**
 * 三维放置信息
 * 3D placement information
 *
 * 由位置坐标和三维形状定义的三维放置，支持包含测试、重叠检测和求交运算。
 * A 3D placement defined by position coordinates and a 3D shape, supporting containment tests, overlap detection, and intersection.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 三维放置信息
 * 3D placement information
 *
 * 由位置坐标和三维形状定义的三维放置，支持包含测试、重叠检测和求交运算。
 * A 3D placement defined by position coordinates and a 3D shape, supporting containment tests, overlap detection, and intersection.
 *
 * @property x x 坐标 / x coordinate
 * @property y y 坐标 / y coordinate
 * @property z z 坐标 / z coordinate
 * @property shape 三维形状 / 3D shape
 * @param V 数值类型 / Number type
 */
data class QuantityPlacement3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val shape: QuantityShape3<V>
) {
    /** 包围盒 / Bounding box */
    val box: QuantityBox3<V>
        get() = QuantityBox3(
            x = x,
            y = y,
            z = z,
            cuboid = shape.boundingCuboid
        )

    /** 放置宽度 / Placement width */
    val width: Quantity<V> get() = box.width
    /** 放置高度 / Placement height */
    val height: Quantity<V> get() = box.height
    /** 放置深度 / Placement depth */
    val depth: Quantity<V> get() = box.depth
    /** x 方向最大值 / Maximum x value */
    val maxX: Quantity<V> get() = box.maxX
    /** y 方向最大值 / Maximum y value */
    val maxY: Quantity<V> get() = box.maxY
    /** z 方向最大值 / Maximum z value */
    val maxZ: Quantity<V> get() = box.maxZ

    /**
     * 判断点是否在放置区域内
     * Check if a point is inside the placement area
     *
     * @param x 点的 x 坐标 / x coordinate of the point
     * @param y 点的 y 坐标 / y coordinate of the point
     * @param z 点的 z 坐标 / z coordinate of the point
     * @param withLowerBound 是否包含下界 / Whether to include lower bound
     * @param withUpperBound 是否包含上界 / Whether to include upper bound
     * @param withBorder 是否包含边界 / Whether to include border
     * @return 点是否在放置区域内 / Whether the point is inside
     */
    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>,
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

    /**
     * 判断两个放置区域是否重叠
     * Check if two placement areas overlap
     *
     * @param rhs 另一个放置区域 / Another placement
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: QuantityPlacement3<V>): Boolean = box.overlapped(rhs.box)

    /**
     * 计算两个放置区域的交集
     * Compute the intersection of two placement areas
     *
     * @param rhs 另一个放置区域 / Another placement
     * @return 交集放置区域，如果不相交则返回 null / Intersection placement, or null if they don't intersect
     */
    fun intersect(rhs: QuantityPlacement3<V>): QuantityPlacement3<V>? {
        val intersected = box.intersect(rhs.box) ?: return null
        return QuantityPlacement3(
            x = intersected.x,
            y = intersected.y,
            z = intersected.z,
            shape = intersected.cuboid
        )
    }
}
