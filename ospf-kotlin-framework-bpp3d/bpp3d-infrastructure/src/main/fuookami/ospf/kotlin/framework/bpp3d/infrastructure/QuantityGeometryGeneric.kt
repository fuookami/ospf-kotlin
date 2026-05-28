package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.minus as quantityMinus
import fuookami.ospf.kotlin.quantities.quantity.plus as quantityPlus
import fuookami.ospf.kotlin.quantities.quantity.times as quantityTimes
import fuookami.ospf.kotlin.utils.functional.Order

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityBinary(
    lhs: Quantity<V>,
    rhs: Quantity<V>,
    flt64Op: (Quantity<InfraNumber>, Quantity<InfraNumber>) -> Quantity<InfraNumber>,
    fltXOp: (Quantity<FltX>, Quantity<FltX>) -> Quantity<FltX>,
    symbol: String
): Quantity<V> {
    return when (lhs.value) {
        is InfraNumber -> flt64Op(lhs as Quantity<InfraNumber>, rhs as Quantity<InfraNumber>) as Quantity<V>
        is FltX -> fltXOp(lhs as Quantity<FltX>, rhs as Quantity<FltX>) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '$symbol': ${lhs.value::class.simpleName}"
        )
    }
}

private fun <V : FloatingNumber<V>> plusQuantity(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityPlus(r) }, { l, r -> l.quantityPlus(r) }, "+")
}

private fun <V : FloatingNumber<V>> minusQuantity(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityMinus(r) }, { l, r -> l.quantityMinus(r) }, "-")
}

private fun <V : FloatingNumber<V>> timesQuantity(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityTimes(r) }, { l, r -> l.quantityTimes(r) }, "*")
}

data class QuantityVector2G<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(rhs: QuantityVector2G<V>): QuantityVector2G<V> {
        return QuantityVector2G(
            x = plusQuantity(x, rhs.x),
            y = plusQuantity(y, rhs.y)
        )
    }

    operator fun minus(rhs: QuantityVector2G<V>): QuantityVector2G<V> {
        return QuantityVector2G(
            x = minusQuantity(x, rhs.x),
            y = minusQuantity(y, rhs.y)
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
            x = plusQuantity(x, rhs.x),
            y = plusQuantity(y, rhs.y),
            z = plusQuantity(z, rhs.z)
        )
    }

    operator fun minus(rhs: QuantityVector3G<V>): QuantityVector3G<V> {
        return QuantityVector3G(
            x = minusQuantity(x, rhs.x),
            y = minusQuantity(y, rhs.y),
            z = minusQuantity(z, rhs.z)
        )
    }
}

data class QuantityPoint2G<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(offset: QuantityVector2G<V>): QuantityPoint2G<V> {
        return QuantityPoint2G(
            x = plusQuantity(x, offset.x),
            y = plusQuantity(y, offset.y)
        )
    }

    operator fun minus(offset: QuantityVector2G<V>): QuantityPoint2G<V> {
        return QuantityPoint2G(
            x = minusQuantity(x, offset.x),
            y = minusQuantity(y, offset.y)
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
            x = plusQuantity(x, offset.x),
            y = plusQuantity(y, offset.y),
            z = plusQuantity(z, offset.z)
        )
    }

    operator fun minus(offset: QuantityVector3G<V>): QuantityPoint3G<V> {
        return QuantityPoint3G(
            x = minusQuantity(x, offset.x),
            y = minusQuantity(y, offset.y),
            z = minusQuantity(z, offset.z)
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

    val width: Quantity<V> get() = minusQuantity(maxX, minX)
    val height: Quantity<V> get() = minusQuantity(maxY, minY)
    val area: Quantity<V> get() = timesQuantity(width, height)

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

fun <V : FloatingNumber<V>> point2G(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityPoint2G<V> {
    return QuantityPoint2G(x = x, y = y)
}

fun <V : FloatingNumber<V>> point3G(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityPoint3G<V> {
    return QuantityPoint3G(x = x, y = y, z = z)
}

fun <V : FloatingNumber<V>> vector2G(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityVector2G<V> {
    return QuantityVector2G(x = x, y = y)
}

fun <V : FloatingNumber<V>> vector3G(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityVector3G<V> {
    return QuantityVector3G(x = x, y = y, z = z)
}

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
