/**
 * 二维包围盒
 * 2D Bounding Box
 *
 * 定义二维几何空间中的包围盒，由位置 (x, y) 和投影形状定义。
 * Defines bounding box in 2D geometric space, defined by position (x, y) and projection shape.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.utils.functional.Order
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
        /** 在原点处创建包围盒 / Create a bounding box at the origin */
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

    /** 判断指定点是否在包围盒内 / Check whether a point is inside the bounding box */
    fun contains(
        x: V,
        y: V,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return when (val s = shape) {
            is Rectangle2 -> quantityContainsInRange(x, this.x, maxX, includeLower, includeUpper, "x")
                    && quantityContainsInRange(y, this.y, maxY, includeLower, includeUpper, "y")

            is Circle2 -> {
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

    /** 判断两个包围盒是否重叠 / Check whether two bounding boxes overlap */
    fun overlapped(rhs: Box2<V>): Boolean {
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

    /** 计算两个包围盒的交集，无交集返回 null / Compute intersection of two boxes, returns null if no overlap */
    fun intersect(rhs: Box2<V>): Box2<V>? {
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
        return Box2(
            x = minX,
            y = minY,
            shape = Rectangle2(
                width = quantityMinus(maxX, minX),
                height = quantityMinus(maxY, minY)
            )
        )
    }

    private fun rectangleOverlapped(rhs: Box2<V>): Boolean {
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

    private fun rectCircleOverlapped(circleBox: Box2<V>, circle: Circle2<V>): Boolean {
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

    private fun circleOverlapped(rhs: Box2<V>, lhs: Circle2<V>, rhsCircle: Circle2<V>): Boolean {
        val dx = quantityMinus(centerX, rhs.centerX)
        val dy = quantityMinus(centerY, rhs.centerY)
        val distance2 = quantityPlus((dx * dx), (dy * dy))
        val reach = quantityPlus(lhs.radius, rhsCircle.radius)
        val reach2 = reach * reach
        val ord = quantityOrd(distance2, reach2, "circle-circle-overlap")
        return ord is Order.Less || ord is Order.Equal
    }
}

/** @see Box2 */
typealias AxisAlignedBox2<V> = Box2<V>
