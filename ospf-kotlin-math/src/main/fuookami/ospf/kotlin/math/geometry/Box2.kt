package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.functional.Order

data class Box2<V : FloatingNumber<V>>(
    val x: V,
    val y: V,
    val shape: Shape2<V>
) {
    companion object {
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

    val width: V
        get() = when (shape) {
            is Rectangle2 -> shape.width
            is Circle2 -> shape.diameter
        }

    val height: V
        get() = when (shape) {
            is Rectangle2 -> shape.height
            is Circle2 -> shape.diameter
        }

    val maxX: V get() = quantityPlus(x, width)
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

typealias AxisAlignedBox2<V> = Box2<V>

