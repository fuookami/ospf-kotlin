/**
 * 二维包围盒
 * 2D bounding box
 *
 * 由位置坐标和投影形状定义的二维包围盒，支持包含测试、重叠检测和求交运算。
 * A 2D bounding box defined by position coordinates and a projection shape, supporting containment tests, overlap detection, and intersection.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 二维包围盒
 * 2D bounding box
 *
 * 由原点坐标和形状定义的二维包围盒，支持包含测试、重叠检测和求交运算。
 * A 2D bounding box defined by origin coordinates and a shape, supporting containment tests, overlap detection, and intersection.
 *
 * @property x 原点的 x 坐标 / x coordinate of the origin
 * @property y 原点的 y 坐标 / y coordinate of the origin
 * @property shape 包围盒的形状（矩形或圆形）/ Shape of the bounding box (rectangle or circle)
 * @param V 数值类型 / Number type
 */
data class QuantityBox2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val shape: QuantityShape2<V>
) {
    companion object {
        /**
         * 在原点创建包围盒
         * Create a bounding box at the origin
         *
         * @param shape 形状 / Shape
         * @param V 数值类型 / Number type
         * @return 原点处的包围盒 / Bounding box at the origin
         */
        fun <V : FloatingNumber<V>> atOrigin(shape: QuantityShape2<V>): QuantityBox2<V> {
            return when (shape) {
                is QuantityRectangle2 -> QuantityBox2(
                    x = quantityZeroOf(shape.width),
                    y = quantityZeroOf(shape.height),
                    shape = shape
                )

                is QuantityCircle2 -> {
                    val zero = quantityZeroOf(shape.radius)
                    QuantityBox2(
                        x = zero,
                        y = zero,
                        shape = shape
                    )
                }
            }
        }
    }

    /** 包围盒宽度 / Bounding box width */
    val width: Quantity<V>
        get() = when (shape) {
            is QuantityRectangle2 -> shape.width
            is QuantityCircle2 -> shape.diameter
        }

    /** 包围盒高度 / Bounding box height */
    val height: Quantity<V>
        get() = when (shape) {
            is QuantityRectangle2 -> shape.height
            is QuantityCircle2 -> shape.diameter
        }

    /** x 方向最大值 / Maximum x value */
    val maxX: Quantity<V> get() = quantityPlus(x, width)
    /** y 方向最大值 / Maximum y value */
    val maxY: Quantity<V> get() = quantityPlus(y, height)

    private val centerX: Quantity<V>
        get() = when (val s = shape) {
            is QuantityRectangle2 -> x
            is QuantityCircle2 -> quantityPlus(x, s.radius)
        }

    private val centerY: Quantity<V>
        get() = when (val s = shape) {
            is QuantityRectangle2 -> y
            is QuantityCircle2 -> quantityPlus(y, s.radius)
        }

    /**
     * 判断点是否在包围盒内
     * Check if a point is inside the bounding box
     *
     * @param x 点的 x 坐标 / x coordinate of the point
     * @param y 点的 y 坐标 / y coordinate of the point
     * @param withLowerBound 是否包含下界 / Whether to include lower bound
     * @param withUpperBound 是否包含上界 / Whether to include upper bound
     * @param withBorder 是否包含边界 / Whether to include border
     * @return 点是否在包围盒内 / Whether the point is inside
     */
    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return when (val s = shape) {
            is QuantityRectangle2 -> quantityContainsInRange(x, this.x, maxX, includeLower, includeUpper, "x")
                    && quantityContainsInRange(y, this.y, maxY, includeLower, includeUpper, "y")

            is QuantityCircle2 -> {
                val dx = quantityMinus(x, centerX)
                val dy = quantityMinus(y, centerY)
                val distance2 = quantityPlus((dx * dx), (dy * dy))
                val radius2 = s.radius * s.radius
                val ord = quantityOrd(distance2, radius2, "circle-contains")
                if (withBorder) {
                    ord is Order.Less || ord is Order.Equal
                } else {
                    ord is Order.Less
                }
            }
        }
    }

    /**
     * 判断两个包围盒是否重叠
     * Check if two bounding boxes overlap
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 是否重叠 / Whether they overlap
     */
    fun overlapped(rhs: QuantityBox2<V>): Boolean {
        return when (val lhsShape = shape) {
            is QuantityRectangle2 -> when (val rhsShape = rhs.shape) {
                is QuantityRectangle2 -> rectangleOverlapped(rhs)
                is QuantityCircle2 -> rectCircleOverlapped(rhs, rhsShape)
            }

            is QuantityCircle2 -> when (val rhsShape = rhs.shape) {
                is QuantityRectangle2 -> rhs.rectCircleOverlapped(this, lhsShape)
                is QuantityCircle2 -> circleOverlapped(rhs, lhsShape, rhsShape)
            }
        }
    }

    /**
     * 计算两个包围盒的交集
     * Compute the intersection of two bounding boxes
     *
     * @param rhs 另一个包围盒 / Another bounding box
     * @return 交集包围盒，如果不相交则返回 null / Intersection box, or null if they don't intersect
     */
    fun intersect(rhs: QuantityBox2<V>): QuantityBox2<V>? {
        val minX = quantityMax(x, rhs.x, "x")
        val maxX = quantityMin(this.maxX, rhs.maxX, "x")
        val minY = quantityMax(y, rhs.y, "y")
        val maxY = quantityMin(this.maxY, rhs.maxY, "y")
        if (quantityOrd(minX, maxX, "x") !is Order.Less) {
            return null
        }
        if (quantityOrd(minY, maxY, "y") !is Order.Less) {
            return null
        }
        return QuantityBox2(
            x = minX,
            y = minY,
            shape = QuantityRectangle2(
                width = quantityMinus(maxX, minX),
                height = quantityMinus(maxY, minY)
            )
        )
    }

    private fun rectangleOverlapped(rhs: QuantityBox2<V>): Boolean {
        if (quantityOrd(maxX, rhs.x, "x") !is Order.Greater) {
            return false
        }
        if (quantityOrd(x, rhs.maxX, "x") !is Order.Less) {
            return false
        }
        if (quantityOrd(maxY, rhs.y, "y") !is Order.Greater) {
            return false
        }
        if (quantityOrd(y, rhs.maxY, "y") !is Order.Less) {
            return false
        }
        return true
    }

    private fun rectCircleOverlapped(circleBox: QuantityBox2<V>, circle: QuantityCircle2<V>): Boolean {
        val circleCenterX = quantityPlus(circleBox.x, circle.radius)
        val circleCenterY = quantityPlus(circleBox.y, circle.radius)
        val closestX = quantityClamp(circleCenterX, x, maxX, "x")
        val closestY = quantityClamp(circleCenterY, y, maxY, "y")
        val dx = quantityMinus(circleCenterX, closestX)
        val dy = quantityMinus(circleCenterY, closestY)
        val distance2 = quantityPlus((dx * dx), (dy * dy))
        val radius2 = circle.radius * circle.radius
        val ord = quantityOrd(distance2, radius2, "rect-circle-overlap")
        return ord is Order.Less || ord is Order.Equal
    }

    private fun circleOverlapped(rhs: QuantityBox2<V>, lhs: QuantityCircle2<V>, rhsCircle: QuantityCircle2<V>): Boolean {
        val dx = quantityMinus(centerX, rhs.centerX)
        val dy = quantityMinus(centerY, rhs.centerY)
        val distance2 = quantityPlus((dx * dx), (dy * dy))
        val reach = quantityPlus(lhs.radius, rhsCircle.radius)
        val reach2 = reach * reach
        val ord = quantityOrd(distance2, reach2, "circle-circle-overlap")
        return ord is Order.Less || ord is Order.Equal
    }
}

/**
 * 二维轴对齐包围盒别名
 * Type alias for 2D axis-aligned bounding box
 */
typealias QuantityAxisAlignedBox2<V> = QuantityBox2<V>
