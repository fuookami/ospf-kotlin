package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.utils.functional.Order

typealias QuantityFlt64 = Quantity<Flt64>

data class QuantityVector2G<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(rhs: QuantityVector2G<V>): QuantityVector2G<V> {
        return QuantityVector2G(
            x = x + rhs.x,
            y = y + rhs.y
        )
    }

    operator fun minus(rhs: QuantityVector2G<V>): QuantityVector2G<V> {
        return QuantityVector2G(
            x = x - rhs.x,
            y = y - rhs.y
        )
    }
}

data class QuantityVector3G<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    operator fun plus(rhs: QuantityVector3G<V>): QuantityVector3G<V> {
        return QuantityVector3G(
            x = x + rhs.x,
            y = y + rhs.y,
            z = z + rhs.z
        )
    }

    operator fun minus(rhs: QuantityVector3G<V>): QuantityVector3G<V> {
        return QuantityVector3G(
            x = x - rhs.x,
            y = y - rhs.y,
            z = z - rhs.z
        )
    }
}

data class QuantityPoint2G<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(offset: QuantityVector2G<V>): QuantityPoint2G<V> {
        return QuantityPoint2G(
            x = x + offset.x,
            y = y + offset.y
        )
    }

    operator fun minus(offset: QuantityVector2G<V>): QuantityPoint2G<V> {
        return QuantityPoint2G(
            x = x - offset.x,
            y = y - offset.y
        )
    }

    infix fun ord(rhs: QuantityPoint2G<V>): Order {
        when (val yOrder = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}
            else -> return yOrder
        }
        return quantityOrd(x, rhs.x, "x")
    }
}

data class QuantityPoint3G<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    operator fun plus(offset: QuantityVector3G<V>): QuantityPoint3G<V> {
        return QuantityPoint3G(
            x = x + offset.x,
            y = y + offset.y,
            z = z + offset.z
        )
    }

    operator fun minus(offset: QuantityVector3G<V>): QuantityPoint3G<V> {
        return QuantityPoint3G(
            x = x - offset.x,
            y = y - offset.y,
            z = z - offset.z
        )
    }

    infix fun ord(rhs: QuantityPoint3G<V>): Order {
        when (val zOrder = quantityOrd(z, rhs.z, "z")) {
            Order.Equal -> {}
            else -> return zOrder
        }
        when (val yOrder = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}
            else -> return yOrder
        }
        return quantityOrd(x, rhs.x, "x")
    }
}

data class QuantityRectangle2G<V : FloatingNumber<V>>(
    val minX: Quantity<V>,
    val minY: Quantity<V>,
    val maxX: Quantity<V>,
    val maxY: Quantity<V>
) {
    init {
        require(quantityOrd(minX, maxX, "x") !is Order.Greater) { "minX should be <= maxX" }
        require(quantityOrd(minY, maxY, "y") !is Order.Greater) { "minY should be <= maxY" }
    }

    val width: Quantity<V> get() = maxX - minX
    val height: Quantity<V> get() = maxY - minY
    val area: Quantity<V> get() = width * height

    fun intersect(rhs: QuantityRectangle2G<V>): QuantityRectangle2G<V>? {
        val left = quantityMax(minX, rhs.minX, "x")
        val right = quantityMin(maxX, rhs.maxX, "x")
        val bottom = quantityMax(minY, rhs.minY, "y")
        val top = quantityMin(maxY, rhs.maxY, "y")
        return if (quantityOrd(left, right, "x") is Order.Less
            && quantityOrd(bottom, top, "y") is Order.Less
        ) {
            QuantityRectangle2G(left, bottom, right, top)
        } else {
            null
        }
    }

    fun intersectArea(rhs: QuantityRectangle2G<V>): Quantity<V>? {
        return intersect(rhs)?.area
    }
}

typealias QuantityVector2 = QuantityVector2G<Flt64>
typealias QuantityVector3 = QuantityVector3G<Flt64>
typealias QuantityPoint2 = QuantityPoint2G<Flt64>
typealias QuantityPoint3 = QuantityPoint3G<Flt64>
typealias QuantityRectangle2 = QuantityRectangle2G<Flt64>

private fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

private fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

private fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}
