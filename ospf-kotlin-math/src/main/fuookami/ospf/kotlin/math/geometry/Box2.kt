/**
 * 二维包围盒
 * 2D Bounding Box
 *
 * 定义二维几何空间中的包围盒，由位置 (x, y) 和投影形状定义。
 * Defines bounding box in 2D geometric space, defined by position (x, y) and projection shape.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 二维包围盒，由位置 (x, y) 和形状定义。
 * 2D bounding box defined by position (x, y) and shape.
 *
 * @param V 数值类型 / The numeric type
 * @property x X 坐标 / X coordinate
 * @property y Y 坐标 / Y coordinate
 * @property shape 包围的二维形状 / The enclosed 2D shape
 */
data class Box2<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val shape: Shape2<V>
) {
    companion object {
        /**
         * 在原点处创建包围盒
         * Create a bounding box at the origin
         *
         * @param V 数值类型 / The numeric type
         * @param shape 二维形状 / The 2D shape
         * @return 原点处的包围盒 / The bounding box at the origin
         */
        fun <V : FloatingNumber<V>> atOrigin(shape: Shape2<V>): Box2<V> {
            return when (shape) {
                is Rectangle2 -> Box2(
                    x = quantityZeroOf(shape.width),
                    y = quantityZeroOf(shape.height),
                    shape = shape
                )

                is Circle2 -> {
                    val zero = quantityZeroOf(shape.radius)
                    Box2(
                        x = zero,
                        y = zero,
                        shape = shape
                    )
                }
            }
        }
    }

    /** 宽度 / Width */
    val width: V
        get() = when (shape) {
            is Rectangle2 -> shape.width
            is Circle2 -> shape.diameter
        }

    /** 高度 / Height */
    val height: V
        get() = when (shape) {
            is Rectangle2 -> shape.height
            is Circle2 -> shape.diameter
        }

    /** X 轴最大值 / Maximum X value */
    val maxX: V get() = quantityPlus(x, width)
    /** Y 轴最大值 / Maximum Y value */
    val maxY: V get() = quantityPlus(y, height)

    private val centerX: V
        get() = when (val s = shape) {
            is Rectangle2 -> x
            is Circle2 -> quantityPlus(x, s.radius)
        }

    private val centerY: V
        get() = when (val s = shape) {
            is Rectangle2 -> y
            is Circle2 -> quantityPlus(y, s.radius)
        }

    /**
     * 判断指定点是否在包围盒内
     * Check whether a point is inside the bounding box
     *
     * @param x X 坐标 / X coordinate
     * @param y Y 坐标 / Y coordinate
     * @param withLowerBound 是否包含下界 / Whether to include the lower bound
     * @param withUpperBound 是否包含上界 / Whether to include the upper bound
     * @param withBorder 是否包含边界 / Whether to include the border
     * @return 点是否在包围盒内 / Whether the point is inside the bounding box
     */
    fun contains(
        x: V,
        y: V,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Ret<Boolean> {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return when (val s = shape) {
            is Rectangle2 -> {
                val xIn = when (val result = quantityContainsInRange(x, this.x, maxX, includeLower, includeUpper, "x")) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (!xIn) {
                    return ok(false)
                }
                quantityContainsInRange(y, this.y, maxY, includeLower, includeUpper, "y")
            }

            is Circle2 -> {
                val dx = quantityMinus(x, centerX)
                val dy = quantityMinus(y, centerY)
                val distance2 = quantityPlus((dx * dx), (dy * dy))
                val radius2 = s.radius * s.radius
                val ord = when (val result = quantityOrdSafe(distance2, radius2, "circle-contains")) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                ok(if (withBorder) {
                    ord is Order.Less || ord is Order.Equal
                } else {
                    ord is Order.Less
                })
            }
        }
    }

    /**
     * 判断两个包围盒是否重叠
     * Check whether two bounding boxes overlap
     *
     * @param rhs 另一个包围盒 / The other bounding box
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: Box2<V>): Ret<Boolean> {
        return when (val lhsShape = shape) {
            is Rectangle2 -> when (val rhsShape = rhs.shape) {
                is Rectangle2 -> rectangleOverlapped(rhs)
                is Circle2 -> rectCircleOverlapped(rhs, rhsShape)
            }

            is Circle2 -> when (val rhsShape = rhs.shape) {
                is Rectangle2 -> rhs.rectCircleOverlapped(this, lhsShape)
                is Circle2 -> circleOverlapped(rhs, lhsShape, rhsShape)
            }
        }
    }

    /**
     * 计算两个包围盒的交集，无交集返回 null
     * Compute intersection of two boxes, returns null if no overlap
     *
     * @param rhs 另一个包围盒 / The other bounding box
     * @return 交集包围盒，无交集返回 null / The intersection box, or null if no overlap
     */
    fun intersect(rhs: Box2<V>): Ret<Box2<V>?> {
        val minX = when (val result = quantityMax(x, rhs.x, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxX = when (val result = quantityMin(this.maxX, rhs.maxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val minY = when (val result = quantityMax(y, rhs.y, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val maxY = when (val result = quantityMin(this.maxY, rhs.maxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val xOrd = when (val result = quantityOrdSafe(minX, maxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (xOrd !is Order.Less) {
            return ok<Box2<V>?>(null)
        }
        val yOrd = when (val result = quantityOrdSafe(minY, maxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (yOrd !is Order.Less) {
            return ok<Box2<V>?>(null)
        }
        return ok(Box2(
            x = minX,
            y = minY,
            shape = Rectangle2(
                width = quantityMinus(maxX, minX),
                height = quantityMinus(maxY, minY)
            )
        ))
    }

    /**
     * 判断两个矩形包围盒是否重叠
     * Check whether two rectangle bounding boxes overlap
     *
     * @param rhs 另一个矩形包围盒 / The other rectangle bounding box
     * @return 是否重叠 / Whether they overlap
     */
    private fun rectangleOverlapped(rhs: Box2<V>): Ret<Boolean> {
        val maxXOrd = when (val result = quantityOrdSafe(maxX, rhs.x, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (maxXOrd !is Order.Greater) {
            return ok(false)
        }
        val xOrd = when (val result = quantityOrdSafe(x, rhs.maxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (xOrd !is Order.Less) {
            return ok(false)
        }
        val maxYOrd = when (val result = quantityOrdSafe(maxY, rhs.y, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (maxYOrd !is Order.Greater) {
            return ok(false)
        }
        val yOrd = when (val result = quantityOrdSafe(y, rhs.maxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (yOrd !is Order.Less) {
            return ok(false)
        }
        return ok(true)
    }

    /**
     * 判断矩形包围盒与圆形包围盒是否重叠
     * Check whether a rectangle bounding box overlaps with a circle bounding box
     *
     * @param circleBox 圆形包围盒 / The circle bounding box
     * @param circle 圆形形状 / The circle shape
     * @return 是否重叠 / Whether they overlap
     */
    private fun rectCircleOverlapped(circleBox: Box2<V>, circle: Circle2<V>): Ret<Boolean> {
        val circleCenterX = quantityPlus(circleBox.x, circle.radius)
        val circleCenterY = quantityPlus(circleBox.y, circle.radius)
        val closestX = when (val result = quantityClamp(circleCenterX, x, maxX, "x")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val closestY = when (val result = quantityClamp(circleCenterY, y, maxY, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val dx = quantityMinus(circleCenterX, closestX)
        val dy = quantityMinus(circleCenterY, closestY)
        val distance2 = quantityPlus((dx * dx), (dy * dy))
        val radius2 = circle.radius * circle.radius
        val ord = when (val result = quantityOrdSafe(distance2, radius2, "rect-circle-overlap")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(ord is Order.Less || ord is Order.Equal)
    }

    /**
     * 判断两个圆形包围盒是否重叠
     * Check whether two circle bounding boxes overlap
     *
     * @param rhs 另一个圆形包围盒 / The other circle bounding box
     * @param lhs 左侧圆形形状 / The left circle shape
     * @param rhsCircle 右侧圆形形状 / The right circle shape
     * @return 是否重叠 / Whether they overlap
     */
    private fun circleOverlapped(rhs: Box2<V>, lhs: Circle2<V>, rhsCircle: Circle2<V>): Ret<Boolean> {
        val dx = quantityMinus(centerX, rhs.centerX)
        val dy = quantityMinus(centerY, rhs.centerY)
        val distance2 = quantityPlus((dx * dx), (dy * dy))
        val reach = quantityPlus(lhs.radius, rhsCircle.radius)
        val reach2 = reach * reach
        val ord = when (val result = quantityOrdSafe(distance2, reach2, "circle-circle-overlap")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(ord is Order.Less || ord is Order.Equal)
    }
}

/**
 * 二维轴对齐包围盒，等同于 Box2。
 * 2D axis-aligned bounding box, equivalent to Box2.
 *
 * @param V 数值类型 / The numeric type
 * @see Box2
 */
typealias AxisAlignedBox2<V> = Box2<V>
