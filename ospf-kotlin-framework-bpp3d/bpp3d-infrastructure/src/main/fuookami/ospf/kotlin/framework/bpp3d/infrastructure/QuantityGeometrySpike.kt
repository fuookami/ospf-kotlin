package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.utils.functional.Order

typealias QuantityFlt64 = Quantity<Flt64>

data class QuantityVector2(
    val x: QuantityFlt64,
    val y: QuantityFlt64
) {
    operator fun plus(rhs: QuantityVector2): QuantityVector2 {
        return QuantityVector2(
            x = x + rhs.x,
            y = y + rhs.y
        )
    }

    operator fun minus(rhs: QuantityVector2): QuantityVector2 {
        return QuantityVector2(
            x = x - rhs.x,
            y = y - rhs.y
        )
    }
}

data class QuantityVector3(
    val x: QuantityFlt64,
    val y: QuantityFlt64,
    val z: QuantityFlt64
) {
    operator fun plus(rhs: QuantityVector3): QuantityVector3 {
        return QuantityVector3(
            x = x + rhs.x,
            y = y + rhs.y,
            z = z + rhs.z
        )
    }

    operator fun minus(rhs: QuantityVector3): QuantityVector3 {
        return QuantityVector3(
            x = x - rhs.x,
            y = y - rhs.y,
            z = z - rhs.z
        )
    }
}

data class QuantityPoint2(
    val x: QuantityFlt64,
    val y: QuantityFlt64
) {
    operator fun plus(offset: QuantityVector2): QuantityPoint2 {
        return QuantityPoint2(
            x = x + offset.x,
            y = y + offset.y
        )
    }

    operator fun minus(offset: QuantityVector2): QuantityPoint2 {
        return QuantityPoint2(
            x = x - offset.x,
            y = y - offset.y
        )
    }

    infix fun ord(rhs: QuantityPoint2): Order {
        when (val yOrder = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}
            else -> return yOrder
        }
        return quantityOrd(x, rhs.x, "x")
    }
}

data class QuantityPoint3(
    val x: QuantityFlt64,
    val y: QuantityFlt64,
    val z: QuantityFlt64
) {
    operator fun plus(offset: QuantityVector3): QuantityPoint3 {
        return QuantityPoint3(
            x = x + offset.x,
            y = y + offset.y,
            z = z + offset.z
        )
    }

    operator fun minus(offset: QuantityVector3): QuantityPoint3 {
        return QuantityPoint3(
            x = x - offset.x,
            y = y - offset.y,
            z = z - offset.z
        )
    }

    infix fun ord(rhs: QuantityPoint3): Order {
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

data class QuantityRectangle2(
    val minX: QuantityFlt64,
    val minY: QuantityFlt64,
    val maxX: QuantityFlt64,
    val maxY: QuantityFlt64
) {
    init {
        require(quantityOrd(minX, maxX, "x") !is Order.Greater) { "minX should be <= maxX" }
        require(quantityOrd(minY, maxY, "y") !is Order.Greater) { "minY should be <= maxY" }
    }

    val width: QuantityFlt64 get() = maxX - minX
    val height: QuantityFlt64 get() = maxY - minY
    val area: QuantityFlt64 get() = width * height

    fun intersect(rhs: QuantityRectangle2): QuantityRectangle2? {
        val left = quantityMax(minX, rhs.minX, "x")
        val right = quantityMin(maxX, rhs.maxX, "x")
        val bottom = quantityMax(minY, rhs.minY, "y")
        val top = quantityMin(maxY, rhs.maxY, "y")
        return if (quantityOrd(left, right, "x") is Order.Less
            && quantityOrd(bottom, top, "y") is Order.Less
        ) {
            QuantityRectangle2(left, bottom, right, top)
        } else {
            null
        }
    }

    fun intersectArea(rhs: QuantityRectangle2): QuantityFlt64? {
        return intersect(rhs)?.area
    }
}

private fun quantityOrd(lhs: QuantityFlt64, rhs: QuantityFlt64, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

private fun quantityMax(lhs: QuantityFlt64, rhs: QuantityFlt64, axis: String): QuantityFlt64 {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

private fun quantityMin(lhs: QuantityFlt64, rhs: QuantityFlt64, axis: String): QuantityFlt64 {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}
