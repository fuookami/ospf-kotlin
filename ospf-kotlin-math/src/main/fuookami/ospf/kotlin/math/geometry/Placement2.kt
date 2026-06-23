/**
 * 二维放置
 * 2D Placement
 *
 * 定义二维几何空间中的放置操作，将投影形状放置在指定 (x, y) 位置。
 * Defines placement operation in 2D geometric space, placing a projection shape at a specified (x, y) position.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret

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

    /**
     * 判断指定点是否在放置区域内
     * Check whether a point is inside the placement region
     *
     * @param x X 坐标 / X coordinate
     * @param y Y 坐标 / Y coordinate
     * @param withLowerBound 是否包含下界 / Whether to include the lower bound
     * @param withUpperBound 是否包含上界 / Whether to include the upper bound
     * @param withBorder 是否包含边界 / Whether to include the border
     * @return 点是否在放置区域内 / Whether the point is inside the placement region
     */
    fun contains(
        x: V,
        y: V,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Ret<Boolean> {
        return box.contains(
            x = x,
            y = y,
            withLowerBound = withLowerBound,
            withUpperBound = withUpperBound,
            withBorder = withBorder
        )
    }

    /**
     * 判断两个放置是否重叠
     * Check whether two placements overlap
     *
     * @param rhs 另一个放置 / The other placement
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: Placement2<V>): Ret<Boolean> {
        return box.overlapped(rhs.box)
    }

    /**
     * 计算两个放置的交集，无交集返回 null
     * Compute intersection of two placements, returns null if no overlap
     *
     * @param rhs 另一个放置 / The other placement
     * @return 交集放置，无交集返回 null / The intersection placement, or null if no overlap
     */
    fun intersect(rhs: Placement2<V>): Ret<Placement2<V>?> {
        val intersection = when (val result = box.intersect(rhs.box)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        } ?: return ok<Placement2<V>?>(null)
        return ok(Placement2(
            x = intersection.x,
            y = intersection.y,
            shape = intersection.shape
        ))
    }
}
