@file:JvmName("GeometryOpsKt")

package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.functional.Order

internal fun <V : FloatingNumber<V>> quantityPlus(lhs: V, rhs: V): V {
    return lhs + rhs
}

internal fun <V : FloatingNumber<V>> quantityMinus(lhs: V, rhs: V): V {
    return lhs - rhs
}

internal fun <V : FloatingNumber<V>> quantityOrd(lhs: V, rhs: V, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable scalar on axis $axis")
}

internal fun <V : FloatingNumber<V>> quantityMax(lhs: V, rhs: V, axis: String): V {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

internal fun <V : FloatingNumber<V>> quantityMin(lhs: V, rhs: V, axis: String): V {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

internal fun <V : FloatingNumber<V>> quantityClamp(
    value: V,
    lb: V,
    ub: V,
    axis: String
): V {
    return when (quantityOrd(value, lb, "${axis}-lb")) {
        is Order.Less -> lb
        else -> when (quantityOrd(value, ub, "${axis}-ub")) {
            is Order.Greater -> ub
            else -> value
        }
    }
}

internal fun <V : FloatingNumber<V>> quantityContainsInRange(
    value: V,
    lb: V,
    ub: V,
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

internal fun <V : FloatingNumber<V>> quantityZeroOf(value: V): V {
    return value.constants.zero
}
