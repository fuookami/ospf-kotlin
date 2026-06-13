/**
 * 泛型量纲几何基础设施。
 * Quantity geometry infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.div as quantityDiv
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.minus as quantityMinus
import fuookami.ospf.kotlin.quantities.quantity.plus as quantityPlus
import fuookami.ospf.kotlin.quantities.quantity.times as quantityTimes
import fuookami.ospf.kotlin.utils.functional.Order

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityBinary(
    lhs: Quantity<V>,
    rhs: Quantity<V>,
    op: (Quantity<FltX>, Quantity<FltX>) -> Quantity<FltX>,
    symbol: String
): Quantity<V> {
    return when (lhs.value) {
        is FltX -> op(lhs as Quantity<FltX>, rhs as Quantity<FltX>) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '$symbol': ${lhs.value::class.simpleName}"
        )
    }
}

internal fun <V : FloatingNumber<V>> quantityPlusByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityPlus(r) }, "+")
}

internal fun <V : FloatingNumber<V>> quantityMinusByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityMinus(r) }, "-")
}

internal fun <V : FloatingNumber<V>> quantityTimesByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityTimes(r) }, "*")
}

internal fun <V : FloatingNumber<V>> quantityZeroByValue(sample: Quantity<V>): Quantity<V> {
    return quantityMinusByValue(sample, sample)
}

@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityScaleByValue(
    quantity: Quantity<V>,
    scale: UInt64
): Quantity<V> {
    return when (quantity.value) {
        is FltX -> ((quantity as Quantity<FltX>).quantityTimes(fltX(scale))) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '*': ${quantity.value::class.simpleName}"
        )
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityScaleByFltXValue(
    quantity: Quantity<V>,
    scale: FltX
): Quantity<V> {
    return when (quantity.value) {
        is FltX -> ((quantity as Quantity<FltX>).quantityTimes(scale)) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '*': ${quantity.value::class.simpleName}"
        )
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityRatioByValue(lhs: Quantity<V>, rhs: Quantity<V>): V {
    return when (lhs.value) {
        is FltX -> ((lhs as Quantity<FltX>).quantityDiv(rhs as Quantity<FltX>).value) as V
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '/': ${lhs.value::class.simpleName}"
        )
    }
}

internal fun <V : FloatingNumber<V>> repeatedQuantitySumByValue(
    sample: Quantity<V>,
    times: UInt64
): Quantity<V> {
    var acc = quantityZeroByValue(sample)
    var i = UInt64.zero
    while (i < times) {
        acc = quantityPlusByValue(acc, sample)
        i += UInt64.one
    }
    return acc
}

data class QuantityVector2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = quantityPlusByValue(x, rhs.x),
            y = quantityPlusByValue(y, rhs.y)
        )
    }

    operator fun minus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = quantityMinusByValue(x, rhs.x),
            y = quantityMinusByValue(y, rhs.y)
        )
    }
}

data class QuantityVector3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    operator fun plus(rhs: QuantityVector3<V>): QuantityVector3<V> {
        return QuantityVector3(
            x = quantityPlusByValue(x, rhs.x),
            y = quantityPlusByValue(y, rhs.y),
            z = quantityPlusByValue(z, rhs.z)
        )
    }

    operator fun minus(rhs: QuantityVector3<V>): QuantityVector3<V> {
        return QuantityVector3(
            x = quantityMinusByValue(x, rhs.x),
            y = quantityMinusByValue(y, rhs.y),
            z = quantityMinusByValue(z, rhs.z)
        )
    }
}

data class QuantityPoint2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = quantityPlusByValue(x, offset.x),
            y = quantityPlusByValue(y, offset.y)
        )
    }

    operator fun minus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = quantityMinusByValue(x, offset.x),
            y = quantityMinusByValue(y, offset.y)
        )
    }

    infix fun ord(rhs: QuantityPoint2<V>): Order {
        when (val yOrder = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}
            else -> return yOrder
        }
        return quantityOrd(x, rhs.x, "x")
    }
}

data class QuantityPoint3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    operator fun plus(offset: QuantityVector3<V>): QuantityPoint3<V> {
        return QuantityPoint3(
            x = quantityPlusByValue(x, offset.x),
            y = quantityPlusByValue(y, offset.y),
            z = quantityPlusByValue(z, offset.z)
        )
    }

    operator fun minus(offset: QuantityVector3<V>): QuantityPoint3<V> {
        return QuantityPoint3(
            x = quantityMinusByValue(x, offset.x),
            y = quantityMinusByValue(y, offset.y),
            z = quantityMinusByValue(z, offset.z)
        )
    }

    infix fun ord(rhs: QuantityPoint3<V>): Order {
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

data class Rectangle2<V : FloatingNumber<V>>(
    val minX: Quantity<V>,
    val minY: Quantity<V>,
    val maxX: Quantity<V>,
    val maxY: Quantity<V>
) {
    init {
        require(quantityOrd(minX, maxX, "x") !is Order.Greater) { "minX should be <= maxX" }
        require(quantityOrd(minY, maxY, "y") !is Order.Greater) { "minY should be <= maxY" }
    }

    val width: Quantity<V> get() = quantityMinusByValue(maxX, minX)
    val height: Quantity<V> get() = quantityMinusByValue(maxY, minY)
    val area: Quantity<V> get() = quantityTimesByValue(width, height)

    fun intersect(rhs: Rectangle2<V>): Rectangle2<V>? {
        val left = quantityMax(minX, rhs.minX, "x")
        val right = quantityMin(maxX, rhs.maxX, "x")
        val bottom = quantityMax(minY, rhs.minY, "y")
        val top = quantityMin(maxY, rhs.maxY, "y")
        return if (quantityOrd(left, right, "x") is Order.Less
            && quantityOrd(bottom, top, "y") is Order.Less
        ) {
            Rectangle2(left, bottom, right, top)
        } else {
            null
        }
    }

    fun intersectArea(rhs: Rectangle2<V>): Quantity<V>? {
        return intersect(rhs)?.area
    }
}

fun <V : FloatingNumber<V>> point2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityPoint2<V> {
    return QuantityPoint2(x = x, y = y)
}

fun <V : FloatingNumber<V>> point3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityPoint3<V> {
    return QuantityPoint3(x = x, y = y, z = z)
}

fun <V : FloatingNumber<V>> vector2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityVector2<V> {
    return QuantityVector2(x = x, y = y)
}

fun <V : FloatingNumber<V>> vector3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityVector3<V> {
    return QuantityVector3(x = x, y = y, z = z)
}

internal fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

internal fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

internal fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}
