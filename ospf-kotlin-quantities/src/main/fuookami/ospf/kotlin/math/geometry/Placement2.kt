/**
 * 二维放置信息
 * 2D placement information
 *
 * 由位置坐标和投影形状定义的二维放置，支持包含测试、重叠检测和求交运算。
 * A 2D placement defined by position coordinates and a projection shape, supporting containment tests, overlap detection, and intersection.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 二维放置信息
 * 2D placement information
 *
 * 由位置坐标和投影形状定义的二维放置，支持包含测试、重叠检测和求交运算。
 * A 2D placement defined by position coordinates and a projection shape, supporting containment tests, overlap detection, and intersection.
 *
 * @property x x 坐标 / x coordinate
 * @property y y 坐标 / y coordinate
 * @property shape 投影形状 / Projection shape
 * @param V 数值类型 / Number type
 */
data class QuantityPlacement2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val shape: QuantityProjection2<V>
) {
    private val box: QuantityBox2<V> get() = QuantityBox2(x = x, y = y, shape = shape)

    /** 放置宽度 / Placement width */
    val width: Quantity<V>
        get() = box.width

    /** 放置高度 / Placement height */
    val height: Quantity<V>
        get() = box.height

    /** x 方向最大值 / Maximum x value */
    val maxX: Quantity<V> get() = box.maxX
    /** y 方向最大值 / Maximum y value */
    val maxY: Quantity<V> get() = box.maxY

    /**
     * 判断点是否在放置区域内
     * Check if a point is inside the placement area
     *
     * @param x 点的 x 坐标 / x coordinate of the point
     * @param y 点的 y 坐标 / y coordinate of the point
     * @param withLowerBound 是否包含下界 / Whether to include lower bound
     * @param withUpperBound 是否包含上界 / Whether to include upper bound
     * @param withBorder 是否包含边界 / Whether to include border
     * @return 点是否在放置区域内 / Whether the point is inside
     */
    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        return box.contains(
            x = x,
            y = y,
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
    fun overlapped(rhs: QuantityPlacement2<V>): Boolean {
        return box.overlapped(rhs.box)
    }

    /**
     * 计算两个放置区域的交集
     * Compute the intersection of two placement areas
     *
     * @param rhs 另一个放置区域 / Another placement
     * @return 交集放置区域，如果不相交则返回 null / Intersection placement, or null if they don't intersect
     */
    fun intersect(rhs: QuantityPlacement2<V>): QuantityPlacement2<V>? {
        val intersection = box.intersect(rhs.box) ?: return null
        return QuantityPlacement2(
            x = intersection.x,
            y = intersection.y,
            shape = intersection.shape
        )
    }
}
