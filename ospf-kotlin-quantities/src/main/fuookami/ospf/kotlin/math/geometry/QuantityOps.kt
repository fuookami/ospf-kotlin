package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.utils.functional.Order

internal fun <V : FloatingNumber<V>> quantityPlus(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    if (lhs.unit == rhs.unit) {
        return Quantity(lhs.value + rhs.value, lhs.unit)
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: throw IllegalArgumentException("Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return Quantity(lhs.value + converted.value, lhs.unit)
}

internal fun <V : FloatingNumber<V>> quantityMinus(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    if (lhs.unit == rhs.unit) {
        return Quantity(lhs.value - rhs.value, lhs.unit)
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: throw IllegalArgumentException("Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return Quantity(lhs.value - converted.value, lhs.unit)
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

internal fun <V : FloatingNumber<V>> quantityClamp(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    axis: String
): Quantity<V> {
    return when (quantityOrd(value, lb, "${axis}-lb")) {
        is Order.Less -> lb
        else -> when (quantityOrd(value, ub, "${axis}-ub")) {
            is Order.Greater -> ub
            else -> value
        }
    }
}

internal fun <V : FloatingNumber<V>> quantityContainsInRange(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    withLowerBound: Boolean,
    withUpperBound: Boolean,
    axis: String
): Boolean {
    val lower = quantityOrd(value, lb, axis)
    val upper = quantityOrd(value, ub, axis)
    val lowerOk = if (withLowerBound) {
        lower is Order.Equal || lower is Order.Greater
    } else {
        lower is Order.Greater
    }
    val upperOk = if (withUpperBound) {
        upper is Order.Equal || upper is Order.Less
    } else {
        upper is Order.Less
    }
    return lowerOk && upperOk
}

internal fun <V : FloatingNumber<V>> quantityZeroOf(quantity: Quantity<V>): Quantity<V> {
    return quantity.value.constants.zero * quantity.unit
}

