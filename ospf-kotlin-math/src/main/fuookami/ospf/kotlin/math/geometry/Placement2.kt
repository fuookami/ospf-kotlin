/**
 * 二维放置
 * 2D Placement
 *
 * 定义二维几何空间中的放置操作，将投影形状放置在指定 (x, y) 位置。
 * Defines placement operation in 2D geometric space, placing a projection shape at a specified (x, y) position.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 二维放置，将一个二维投影形状放置在 (x, y) 位置。
 * 2D placement: places a 2D projection shape at position (x, y).
 *
 * @param V 数值类型 / The numeric type
 * @property x X 坐标 / X coordinate
 * @property y Y 坐标 / Y coordinate
 * @property shape 放置的二维形状 / The 2D shape being placed
 */
data class Placement2<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val shape: Projection2<V>
) {
    private val box: Box2<V> get() = Box2(x = x, y = y, shape = shape)

    /** 宽度 / Width */
    val width: V
        get() = box.width

    /** 高度 / Height */
    val height: V
        get() = box.height

    /** X 轴最大值 / Maximum X value */
    val maxX: V get() = box.maxX
    /** Y 轴最大值 / Maximum Y value */
    val maxY: V get() = box.maxY

    /** 判断指定点是否在放置区域内 / Check whether a point is inside the placement region */
    fun contains(
        x: V,
        y: V,
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

    /** 判断两个放置是否重叠 / Check whether two placements overlap */
    fun overlapped(rhs: Placement2<V>): Boolean {
        return box.overlapped(rhs.box)
    }

    /** 计算两个放置的交集，无交集返回 null / Compute intersection of two placements, returns null if no overlap */
    fun intersect(rhs: Placement2<V>): Placement2<V>? {
        val intersection = box.intersect(rhs.box) ?: return null
        return Placement2(
            x = intersection.x,
            y = intersection.y,
            shape = intersection.shape
        )
    }
}
